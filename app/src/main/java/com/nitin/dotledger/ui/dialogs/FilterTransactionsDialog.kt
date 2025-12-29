package com.nitin.dotledger.ui.dialogs

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.nitin.dotledger.R
import com.nitin.dotledger.data.entities.TransactionType
import com.nitin.dotledger.data.models.*
import com.nitin.dotledger.databinding.DialogFilterTransactionsBinding
import com.nitin.dotledger.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

class FilterTransactionsDialog : BottomSheetDialogFragment() {
    private var _binding: DialogFilterTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    private var selectedTypes = mutableSetOf<TransactionType>()
    private var selectedAccountIds = mutableSetOf<Long>()
    private var selectedCategoryIds = mutableSetOf<Long>()
    private var selectedDateRange: DateRange? = null
    private var customStartDate: Long? = null
    private var customEndDate: Long? = null
    private var selectedSortOption = SortOption.DATE_DESC
    private var searchQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFilterTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSortByDropdown()
        loadAccountsAndCategories()
        loadCurrentFilter()
        setupClickListeners()
    }

    private fun setupSortByDropdown() {
        val sortOptions = SortOption.values().map { it.displayName }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            sortOptions
        )
        binding.etSortBy.setAdapter(adapter)
        binding.etSortBy.setText(SortOption.DATE_DESC.displayName, false)
    }

    private fun loadAccountsAndCategories() {
        // Load accounts
        viewModel.allAccounts.observe(viewLifecycleOwner) { accounts ->
            binding.chipGroupAccounts.removeAllViews()

            accounts.forEach { account ->
                val chip = Chip(requireContext()).apply {
                    text = account.name
                    isCheckable = true
                    setChipBackgroundColorResource(R.color.chip_background_selector)
                    setChipStrokeColorResource(R.color.chip_stroke_selector)
                    chipStrokeWidth = 2f
                    setTextAppearance(R.style.TextAppearance_DotLedger_Body1)
                    setTextColor(resources.getColor(R.color.white, null))

                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            selectedAccountIds.add(account.id)
                        } else {
                            selectedAccountIds.remove(account.id)
                        }
                    }
                }
                binding.chipGroupAccounts.addView(chip)
            }
        }

        // Load categories
        viewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            binding.chipGroupCategories.removeAllViews()

            categories.forEach { category ->
                val chip = Chip(requireContext()).apply {
                    text = category.name
                    isCheckable = true
                    setChipBackgroundColorResource(R.color.chip_background_selector)
                    setChipStrokeColorResource(R.color.chip_stroke_selector)
                    chipStrokeWidth = 2f
                    setTextAppearance(R.style.TextAppearance_DotLedger_Body1)
                    setTextColor(resources.getColor(R.color.white, null))

                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            selectedCategoryIds.add(category.id)
                        } else {
                            selectedCategoryIds.remove(category.id)
                        }
                    }
                }
                binding.chipGroupCategories.addView(chip)
            }
        }
    }

    private fun loadCurrentFilter() {
        val currentFilter = viewModel.currentFilter.value
        if (currentFilter != null && currentFilter.hasActiveFilters()) {
            // Load search query
            searchQuery = currentFilter.searchQuery
            binding.etSearchNotes.setText(searchQuery)

            // Load types
            if (currentFilter.types.contains(TransactionType.INCOME)) {
                binding.chipTypeIncome.isChecked = true
                selectedTypes.add(TransactionType.INCOME)
            }
            if (currentFilter.types.contains(TransactionType.EXPENSE)) {
                binding.chipTypeExpense.isChecked = true
                selectedTypes.add(TransactionType.EXPENSE)
            }
            if (currentFilter.types.contains(TransactionType.TRANSFER)) {
                binding.chipTypeTransfer.isChecked = true
                selectedTypes.add(TransactionType.TRANSFER)
            }

            // Load accounts
            selectedAccountIds.addAll(currentFilter.accountIds)

            // Load categories
            selectedCategoryIds.addAll(currentFilter.categoryIds)

            // Load date range
            currentFilter.dateRange?.let { range ->
                selectedDateRange = range
                binding.tvStartDate.text = dateFormat.format(Date(range.startDate))
                binding.tvEndDate.text = dateFormat.format(Date(range.endDate))
            }

            // Load amount range
            currentFilter.amountRange?.let { range ->
                if (range.minAmount > 0) {
                    binding.etMinAmount.setText(range.minAmount.toString())
                }
                if (range.maxAmount < Double.MAX_VALUE) {
                    binding.etMaxAmount.setText(range.maxAmount.toString())
                }
            }

            // Load sort option
            binding.etSortBy.setText(currentFilter.sortBy.displayName, false)
            selectedSortOption = currentFilter.sortBy
        }
    }

    private fun setupClickListeners() {
        // Type chips
        binding.chipTypeIncome.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedTypes.add(TransactionType.INCOME)
            } else {
                selectedTypes.remove(TransactionType.INCOME)
            }
        }

        binding.chipTypeExpense.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedTypes.add(TransactionType.EXPENSE)
            } else {
                selectedTypes.remove(TransactionType.EXPENSE)
            }
        }

        binding.chipTypeTransfer.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedTypes.add(TransactionType.TRANSFER)
            } else {
                selectedTypes.remove(TransactionType.TRANSFER)
            }
        }

        // Account select all/clear
        binding.btnSelectAllAccounts.setOnClickListener {
            for (i in 0 until binding.chipGroupAccounts.childCount) {
                val chip = binding.chipGroupAccounts.getChildAt(i) as? Chip
                chip?.isChecked = true
            }
        }

        binding.btnClearAccounts.setOnClickListener {
            for (i in 0 until binding.chipGroupAccounts.childCount) {
                val chip = binding.chipGroupAccounts.getChildAt(i) as? Chip
                chip?.isChecked = false
            }
        }

        // Category select all/clear
        binding.btnSelectAllCategories.setOnClickListener {
            for (i in 0 until binding.chipGroupCategories.childCount) {
                val chip = binding.chipGroupCategories.getChildAt(i) as? Chip
                chip?.isChecked = true
            }
        }

        binding.btnClearCategories.setOnClickListener {
            for (i in 0 until binding.chipGroupCategories.childCount) {
                val chip = binding.chipGroupCategories.getChildAt(i) as? Chip
                chip?.isChecked = false
            }
        }

        // Date range chips
        binding.chipDateToday.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedDateRange = DateRange.today()
                updateDateDisplay()
            }
        }

        binding.chipDateWeek.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedDateRange = DateRange.thisWeek()
                updateDateDisplay()
            }
        }

        binding.chipDateMonth.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedDateRange = DateRange.thisMonth()
                updateDateDisplay()
            }
        }

        binding.chipDate30days.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedDateRange = DateRange.last30Days()
                updateDateDisplay()
            }
        }

        // Amount range chips
        binding.chipAmountSmall.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.etMinAmount.setText("0")
                binding.etMaxAmount.setText("100")
            }
        }

        binding.chipAmountMedium.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.etMinAmount.setText("100")
                binding.etMaxAmount.setText("1000")
            }
        }

        binding.chipAmountLarge.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.etMinAmount.setText("1000")
                binding.etMaxAmount.setText("10000")
            }
        }

        binding.chipAmountXlarge.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.etMinAmount.setText("10000")
                binding.etMaxAmount.setText("")
            }
        }

        // Custom date pickers
        binding.btnStartDate.setOnClickListener {
            showDatePicker(true)
        }

        binding.btnEndDate.setOnClickListener {
            showDatePicker(false)
        }

        // Sort by
        binding.etSortBy.setOnItemClickListener { _, _, position, _ ->
            selectedSortOption = SortOption.values()[position]
        }

        // Clear filters
        binding.btnClearFilters.setOnClickListener {
            clearAllFilters()
        }

        // Cancel
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Apply
        binding.btnApply.setOnClickListener {
            applyFilters()
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val currentDate = if (isStartDate) customStartDate else customEndDate
        if (currentDate != null) {
            calendar.timeInMillis = currentDate
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                if (isStartDate) {
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    customStartDate = calendar.timeInMillis
                    binding.tvStartDate.text = dateFormat.format(Date(customStartDate!!))
                } else {
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    customEndDate = calendar.timeInMillis
                    binding.tvEndDate.text = dateFormat.format(Date(customEndDate!!))
                }

                // If both dates are set, create custom range
                if (customStartDate != null && customEndDate != null) {
                    selectedDateRange = DateRange.custom(customStartDate!!, customEndDate!!)
                    // Uncheck preset date chips
                    binding.chipGroupDate.clearCheck()
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateDisplay() {
        selectedDateRange?.let { range ->
            binding.tvStartDate.text = dateFormat.format(Date(range.startDate))
            binding.tvEndDate.text = dateFormat.format(Date(range.endDate))
            customStartDate = range.startDate
            customEndDate = range.endDate
        }
    }

    private fun clearAllFilters() {
        selectedTypes.clear()
        selectedAccountIds.clear()
        selectedCategoryIds.clear()
        selectedDateRange = null
        customStartDate = null
        customEndDate = null
        selectedSortOption = SortOption.DATE_DESC
        searchQuery = ""

        binding.etSearchNotes.text?.clear()
        binding.chipGroupType.clearCheck()

        // Clear account chips
        for (i in 0 until binding.chipGroupAccounts.childCount) {
            val chip = binding.chipGroupAccounts.getChildAt(i) as? Chip
            chip?.isChecked = false
        }

        // Clear category chips
        for (i in 0 until binding.chipGroupCategories.childCount) {
            val chip = binding.chipGroupCategories.getChildAt(i) as? Chip
            chip?.isChecked = false
        }

        binding.chipGroupDate.clearCheck()
        binding.chipGroupAmount.clearCheck()
        binding.etMinAmount.text?.clear()
        binding.etMaxAmount.text?.clear()
        binding.tvStartDate.text = "Select Date"
        binding.tvEndDate.text = "Select Date"
        binding.etSortBy.setText(SortOption.DATE_DESC.displayName, false)
    }

    private fun applyFilters() {
        searchQuery = binding.etSearchNotes.text.toString().trim()

        val minAmount = binding.etMinAmount.text.toString().toDoubleOrNull() ?: 0.0
        val maxAmount = binding.etMaxAmount.text.toString().toDoubleOrNull() ?: Double.MAX_VALUE

        val amountRange = if (minAmount > 0 || maxAmount < Double.MAX_VALUE) {
            AmountRange(minAmount, maxAmount)
        } else {
            null
        }

        val filter = TransactionFilter(
            searchQuery = searchQuery,
            types = selectedTypes,
            accountIds = selectedAccountIds,
            categoryIds = selectedCategoryIds,
            dateRange = selectedDateRange,
            amountRange = amountRange,
            sortBy = selectedSortOption
        )

        viewModel.applyFilter(filter)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}