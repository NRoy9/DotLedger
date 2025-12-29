package com.nitin.dotledger.ui.dialogs

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.nitin.dotledger.R
import com.nitin.dotledger.data.entities.*
import com.nitin.dotledger.databinding.DialogAddRecurringTransactionBinding
import com.nitin.dotledger.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddRecurringTransactionDialog : DialogFragment() {
    private var _binding: DialogAddRecurringTransactionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    private var editRecurringId: Long? = null
    private var selectedType = TransactionType.EXPENSE
    private var selectedFrequency = RecurringFrequency.MONTHLY
    private var selectedAccountId: Long? = null
    private var selectedCategoryId: Long? = null
    private var selectedStartDate = Calendar.getInstance()
    private var selectedEndDate: Calendar? = null
    private var hasEndDate = false

    private var accounts: List<Account> = emptyList()
    private var categories: List<Category> = emptyList()

    companion object {
        private const val ARG_RECURRING_ID = "recurring_id"

        fun newInstance(recurringId: Long? = null): AddRecurringTransactionDialog {
            val fragment = AddRecurringTransactionDialog()
            recurringId?.let {
                val args = Bundle()
                args.putLong(ARG_RECURRING_ID, it)
                fragment.arguments = args
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddRecurringTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
        updateDateDisplays()

        editRecurringId = arguments?.getLong(ARG_RECURRING_ID)
        if (editRecurringId != null && editRecurringId!! > 0) {
            loadRecurringTransaction()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private fun setupObservers() {
        viewModel.allAccounts.observe(viewLifecycleOwner) { accountList ->
            accounts = accountList
            setupAccountDropdown()
        }

        viewModel.allCategories.observe(viewLifecycleOwner) { categoryList ->
            categories = categoryList
            setupCategoryDropdown()
        }
    }

    private fun setupAccountDropdown() {
        val accountNames = accounts.map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            accountNames
        )
        binding.etAccount.setAdapter(adapter)

        if (accountNames.isNotEmpty() && selectedAccountId == null) {
            binding.etAccount.setText(accountNames[0], false)
            selectedAccountId = accounts[0].id
        }
    }

    private fun setupCategoryDropdown() {
        updateCategoryDropdown()
    }

    private fun updateCategoryDropdown() {
        val categoryType = when (selectedType) {
            TransactionType.EXPENSE -> CategoryType.EXPENSE
            TransactionType.INCOME -> CategoryType.INCOME
            TransactionType.TRANSFER -> {
                binding.tilCategory.visibility = View.GONE
                return
            }
        }

        binding.tilCategory.visibility = View.VISIBLE
        val filteredCategories = categories.filter { it.type == categoryType }
        val categoryNames = filteredCategories.map { it.name }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categoryNames
        )
        binding.etCategory.setAdapter(adapter)

        if (categoryNames.isNotEmpty() && selectedCategoryId == null) {
            binding.etCategory.setText(categoryNames[0], false)
            selectedCategoryId = filteredCategories[0].id
        }
    }

    private fun setupClickListeners() {
        // Type selection
        binding.chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedType = when (checkedIds.firstOrNull()) {
                R.id.chip_income -> TransactionType.INCOME
                R.id.chip_transfer -> TransactionType.TRANSFER
                else -> TransactionType.EXPENSE
            }
            updateCategoryDropdown()
        }

        // Frequency selection
        binding.chipGroupFrequency.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedFrequency = when (checkedIds.firstOrNull()) {
                R.id.chip_daily -> RecurringFrequency.DAILY
                R.id.chip_weekly -> RecurringFrequency.WEEKLY
                R.id.chip_biweekly -> RecurringFrequency.BIWEEKLY
                R.id.chip_quarterly -> RecurringFrequency.QUARTERLY
                R.id.chip_yearly -> RecurringFrequency.YEARLY
                else -> RecurringFrequency.MONTHLY
            }
        }

        // Account selection
        binding.etAccount.setOnItemClickListener { _, _, position, _ ->
            selectedAccountId = accounts[position].id
        }

        // Category selection
        binding.etCategory.setOnItemClickListener { _, _, position, _ ->
            val categoryType = when (selectedType) {
                TransactionType.EXPENSE -> CategoryType.EXPENSE
                TransactionType.INCOME -> CategoryType.INCOME
                else -> return@setOnItemClickListener
            }
            val filteredCategories = categories.filter { it.type == categoryType }
            selectedCategoryId = filteredCategories[position].id
        }

        // Start date picker
        binding.btnStartDate.setOnClickListener {
            showDatePicker(true)
        }

        // End date toggle
        binding.switchHasEndDate.setOnCheckedChangeListener { _, isChecked ->
            hasEndDate = isChecked
            binding.btnEndDate.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                selectedEndDate = null
            }
        }

        // End date picker
        binding.btnEndDate.setOnClickListener {
            showDatePicker(false)
        }

        // Cancel
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Save
        binding.btnSave.setOnClickListener {
            saveRecurringTransaction()
        }

        // Delete
        binding.btnDelete.setOnClickListener {
            deleteRecurringTransaction()
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = if (isStartDate) selectedStartDate else (selectedEndDate ?: Calendar.getInstance())

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                if (isStartDate) {
                    selectedStartDate.set(year, month, day)
                    selectedStartDate.set(Calendar.HOUR_OF_DAY, 0)
                    selectedStartDate.set(Calendar.MINUTE, 0)
                    selectedStartDate.set(Calendar.SECOND, 0)
                } else {
                    if (selectedEndDate == null) {
                        selectedEndDate = Calendar.getInstance()
                    }
                    selectedEndDate?.set(year, month, day)
                    selectedEndDate?.set(Calendar.HOUR_OF_DAY, 23)
                    selectedEndDate?.set(Calendar.MINUTE, 59)
                    selectedEndDate?.set(Calendar.SECOND, 59)
                }
                updateDateDisplays()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateDisplays() {
        binding.tvStartDate.text = dateFormat.format(selectedStartDate.time)
        selectedEndDate?.let {
            binding.tvEndDate.text = dateFormat.format(it.time)
        }
    }

    private fun loadRecurringTransaction() {
        editRecurringId?.let { id ->
            viewModel.getRecurringTransactionById(id).observe(viewLifecycleOwner) { recurring ->
                recurring?.let {
                    binding.tvDialogTitle.text = "EDIT RECURRING TRANSACTION"
                    binding.btnSave.text = "UPDATE"
                    binding.btnDelete.visibility = View.VISIBLE

                    binding.etName.setText(it.name)
                    binding.etAmount.setText(it.amount.toString())
                    binding.etNote.setText(it.note)

                    selectedType = it.type
                    when (it.type) {
                        TransactionType.INCOME -> binding.chipIncome.isChecked = true
                        TransactionType.EXPENSE -> binding.chipExpense.isChecked = true
                        TransactionType.TRANSFER -> binding.chipTransfer.isChecked = true
                    }

                    selectedFrequency = it.frequency
                    when (it.frequency) {
                        RecurringFrequency.DAILY -> binding.chipDaily.isChecked = true
                        RecurringFrequency.WEEKLY -> binding.chipWeekly.isChecked = true
                        RecurringFrequency.BIWEEKLY -> binding.chipBiweekly.isChecked = true
                        RecurringFrequency.MONTHLY -> binding.chipMonthly.isChecked = true
                        RecurringFrequency.QUARTERLY -> binding.chipQuarterly.isChecked = true
                        RecurringFrequency.YEARLY -> binding.chipYearly.isChecked = true
                    }

                    selectedAccountId = it.accountId
                    val account = accounts.find { acc -> acc.id == it.accountId }
                    account?.let { acc -> binding.etAccount.setText(acc.name, false) }

                    selectedCategoryId = it.categoryId
                    val category = categories.find { cat -> cat.id == it.categoryId }
                    category?.let { cat -> binding.etCategory.setText(cat.name, false) }

                    selectedStartDate.timeInMillis = it.startDate

                    if (it.endDate != null) {
                        hasEndDate = true
                        binding.switchHasEndDate.isChecked = true
                        selectedEndDate = Calendar.getInstance().apply {
                            timeInMillis = it.endDate
                        }
                    }

                    updateDateDisplays()
                }
            }
        }
    }

    private fun saveRecurringTransaction() {
        val name = binding.etName.text.toString().trim()
        val amountStr = binding.etAmount.text.toString()
        val note = binding.etNote.text.toString().trim()

        // Validation
        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.tilAmount.error = "Valid amount is required"
            return
        }

        if (selectedAccountId == null) {
            Toast.makeText(requireContext(), "Please select an account", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedType != TransactionType.TRANSFER && selectedCategoryId == null) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        // Calculate next occurrence
        val nextOccurrence = selectedFrequency.getNextOccurrence(selectedStartDate.timeInMillis)

        val recurringTransaction = RecurringTransaction(
            id = editRecurringId ?: 0,
            name = name,
            accountId = selectedAccountId!!,
            categoryId = if (selectedType == TransactionType.TRANSFER) null else selectedCategoryId,
            amount = amount,
            type = selectedType,
            frequency = selectedFrequency,
            startDate = selectedStartDate.timeInMillis,
            endDate = selectedEndDate?.timeInMillis,
            nextOccurrence = nextOccurrence,
            note = note,
            isActive = true
        )

        if (editRecurringId != null && editRecurringId!! > 0) {
            viewModel.updateRecurringTransaction(recurringTransaction)
            Toast.makeText(requireContext(), "Recurring transaction updated", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insertRecurringTransaction(recurringTransaction)
            Toast.makeText(requireContext(), "Recurring transaction created", Toast.LENGTH_SHORT).show()
        }

        dismiss()
    }

    private fun deleteRecurringTransaction() {
        editRecurringId?.let { id ->
            lifecycleScope.launch {
                val recurring = viewModel.getRecurringTransactionByIdSync(id)
                recurring?.let {
                    viewModel.deleteRecurringTransaction(it)
                    Toast.makeText(requireContext(), "Recurring transaction deleted", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}