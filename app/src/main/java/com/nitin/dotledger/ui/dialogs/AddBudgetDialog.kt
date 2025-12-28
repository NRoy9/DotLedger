package com.nitin.dotledger.ui.dialogs

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
import com.nitin.dotledger.data.entities.Budget
import com.nitin.dotledger.data.entities.BudgetPeriod
import com.nitin.dotledger.data.entities.CategoryType
import com.nitin.dotledger.databinding.DialogAddBudgetBinding
import com.nitin.dotledger.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.*

class AddBudgetDialog : DialogFragment() {
    private var _binding: DialogAddBudgetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private var editBudget: Budget? = null
    private var selectedCategoryId: Long? = null
    private var selectedPeriod = BudgetPeriod.MONTHLY

    companion object {
        private const val ARG_BUDGET_ID = "budget_id"

        fun newInstance(budgetId: Long? = null): AddBudgetDialog {
            val fragment = AddBudgetDialog()
            budgetId?.let {
                val args = Bundle()
                args.putLong(ARG_BUDGET_ID, it)
                fragment.arguments = args
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryDropdown()
        loadBudgetIfEditing()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupCategoryDropdown() {
        viewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            // Filter only expense categories
            val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }

            val categoryNames = mutableListOf("Overall Budget (All Categories)")
            categoryNames.addAll(expenseCategories.map { it.name })

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categoryNames
            )

            binding.etCategory.setAdapter(adapter)
            binding.etCategory.setText(categoryNames[0], false)

            binding.etCategory.setOnItemClickListener { _, _, position, _ ->
                selectedCategoryId = if (position == 0) {
                    null // Overall budget
                } else {
                    expenseCategories[position - 1].id
                }
            }
        }
    }

    private fun loadBudgetIfEditing() {
        val budgetId = arguments?.getLong(ARG_BUDGET_ID)

        if (budgetId != null && budgetId > 0) {
            binding.tvDialogTitle.text = "EDIT BUDGET"
            binding.btnSave.text = "UPDATE"
            binding.btnDelete.visibility = View.VISIBLE

            lifecycleScope.launch {
                val budget = viewModel.getBudgetById(budgetId)
                budget?.let {
                    editBudget = it
                    selectedCategoryId = it.categoryId
                    selectedPeriod = it.period

                    // Set category
                    val categories = viewModel.allCategories.value?.filter { cat ->
                        cat.type == CategoryType.EXPENSE
                    } ?: emptyList()

                    val categoryName = if (it.categoryId == null) {
                        "Overall Budget (All Categories)"
                    } else {
                        categories.find { cat -> cat.id == it.categoryId }?.name ?: ""
                    }
                    binding.etCategory.setText(categoryName, false)

                    // Set amount
                    binding.etBudgetAmount.setText(it.amount.toString())

                    // Set period
                    when (it.period) {
                        BudgetPeriod.MONTHLY -> binding.chipMonthly.isChecked = true
                        BudgetPeriod.YEARLY -> binding.chipYearly.isChecked = true
                    }

                    // Set alert percentage
                    binding.etAlertPercentage.setText(it.alertPercentage.toString())
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedPeriod = when (checkedIds.firstOrNull()) {
                R.id.chip_yearly -> BudgetPeriod.YEARLY
                R.id.chip_monthly -> BudgetPeriod.MONTHLY
                else -> BudgetPeriod.MONTHLY
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveBudget()
        }

        binding.btnDelete.setOnClickListener {
            deleteBudget()
        }
    }

    private fun saveBudget() {
        val amountString = binding.etBudgetAmount.text.toString().trim()
        val alertPercentageString = binding.etAlertPercentage.text.toString().trim()

        // Validation
        if (amountString.isEmpty()) {
            binding.tilBudgetAmount.error = "Budget amount is required"
            return
        }

        val amount = amountString.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.tilBudgetAmount.error = "Please enter a valid amount"
            return
        }

        val alertPercentage = alertPercentageString.toIntOrNull() ?: 80
        if (alertPercentage < 0 || alertPercentage > 100) {
            binding.tilAlertPercentage.error = "Alert percentage must be between 0 and 100"
            return
        }

        // Calculate start and end dates based on period
        val calendar = Calendar.getInstance()
        val startDate: Long
        val endDate: Long

        when (selectedPeriod) {
            BudgetPeriod.MONTHLY -> {
                // Start of current month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis

                // End of current month
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                endDate = calendar.timeInMillis
            }
            BudgetPeriod.YEARLY -> {
                // Start of current year
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis

                // End of current year
                calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                endDate = calendar.timeInMillis
            }
        }

        if (editBudget != null) {
            // Update existing budget
            val updatedBudget = editBudget!!.copy(
                categoryId = selectedCategoryId,
                amount = amount,
                period = selectedPeriod,
                startDate = startDate,
                endDate = endDate,
                alertPercentage = alertPercentage,
                modifiedAt = System.currentTimeMillis()
            )
            viewModel.updateBudget(updatedBudget)
            Toast.makeText(requireContext(), "Budget updated", Toast.LENGTH_SHORT).show()
        } else {
            // Create new budget
            val newBudget = Budget(
                categoryId = selectedCategoryId,
                amount = amount,
                period = selectedPeriod,
                startDate = startDate,
                endDate = endDate,
                alertPercentage = alertPercentage
            )
            viewModel.insertBudget(newBudget)
            Toast.makeText(requireContext(), "Budget created", Toast.LENGTH_SHORT).show()
        }

        dismiss()
    }

    private fun deleteBudget() {
        editBudget?.let {
            viewModel.deleteBudget(it)
            Toast.makeText(requireContext(), "Budget deleted", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}