package com.nitin.dotledger.ui.dialogs

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import com.nitin.dotledger.databinding.DialogAddTransactionBinding
import com.nitin.dotledger.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionDialog : DialogFragment() {
    private var _binding: DialogAddTransactionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private var editTransaction: Transaction? = null

    private var selectedDate = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    private var accounts: List<Account> = emptyList()
    private var categories: List<Category> = emptyList()
    private var selectedAccountId: Long? = null
    private var selectedToAccountId: Long? = null
    private var selectedCategoryId: Long? = null
    private var currentType = TransactionType.EXPENSE

    private var currentAmount = "0"

    companion object {
        private const val ARG_TRANSACTION_ID = "transaction_id"
        private const val ARG_TYPE = "type"

        fun newInstance(
            transactionId: Long? = null,
            type: TransactionType = TransactionType.EXPENSE
        ): AddTransactionDialog {
            val fragment = AddTransactionDialog()
            val args = Bundle()
            transactionId?.let { args.putLong(ARG_TRANSACTION_ID, it) }
            args.putString(ARG_TYPE, type.name)
            fragment.arguments = args
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
        _binding = DialogAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Load default transaction type from settings
        val typeString = arguments?.getString(ARG_TYPE)
        val transactionId = arguments?.getLong(ARG_TRANSACTION_ID, -1L) ?: -1L

        if (typeString != null) {
            // Explicit type provided
            currentType = TransactionType.valueOf(typeString)
            setupInitialType()
            setupObservers()
            setupClickListeners()
            updateDateDisplay()
            loadTransactionIfEditing()
        } else if (transactionId > 0) {
            // Editing existing transaction
            setupInitialType()
            setupObservers()
            setupClickListeners()
            updateDateDisplay()
            loadTransactionIfEditing()
        } else {
            // New transaction - use settings default
            viewModel.appSettings.observe(viewLifecycleOwner) { settings ->
                if (settings != null) {
                    currentType = settings.defaultTransactionType
                    setupInitialType()
                }
            }
            setupObservers()
            setupClickListeners()
            updateDateDisplay()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private fun setupInitialType() {
        updateTypeSelection()
        updateUIForType()
    }

    private fun updateTypeSelection() {
        binding.chipExpense.setBackgroundResource(
            if (currentType == TransactionType.EXPENSE) R.drawable.bg_type_selected
            else android.R.color.transparent
        )
        binding.chipIncome.setBackgroundResource(
            if (currentType == TransactionType.INCOME) R.drawable.bg_type_selected
            else android.R.color.transparent
        )
        binding.chipTransfer.setBackgroundResource(
            if (currentType == TransactionType.TRANSFER) R.drawable.bg_type_selected
            else android.R.color.transparent
        )

        binding.chipExpense.setTextColor(
            if (currentType == TransactionType.EXPENSE) Color.WHITE
            else Color.parseColor("#8E8E93")
        )
        binding.chipIncome.setTextColor(
            if (currentType == TransactionType.INCOME) Color.WHITE
            else Color.parseColor("#8E8E93")
        )
        binding.chipTransfer.setTextColor(
            if (currentType == TransactionType.TRANSFER) Color.WHITE
            else Color.parseColor("#8E8E93")
        )
    }

    private fun setupObservers() {
        viewModel.allAccounts.observe(viewLifecycleOwner) { accountsList ->
            accounts = accountsList
        }

        viewModel.allCategories.observe(viewLifecycleOwner) { categoriesList ->
            categories = categoriesList
        }
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveTransaction()
        }

        binding.chipExpense.setOnClickListener {
            currentType = TransactionType.EXPENSE
            updateTypeSelection()
            updateUIForType()
            clearCategorySelection()
        }

        binding.chipIncome.setOnClickListener {
            currentType = TransactionType.INCOME
            updateTypeSelection()
            updateUIForType()
            clearCategorySelection()
        }

        binding.chipTransfer.setOnClickListener {
            currentType = TransactionType.TRANSFER
            updateTypeSelection()
            updateUIForType()
            clearCategorySelection()
        }

        binding.btnDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnAccount.setOnClickListener {
            showAccountPicker()
        }

        binding.btnCategory.setOnClickListener {
            if (currentType == TransactionType.TRANSFER) {
                showToAccountPicker()
            } else {
                showCategoryPicker()
            }
        }

        setupCalculatorButtons()
    }

    private fun setupCalculatorButtons() {
        binding.btnNum0.setOnClickListener { appendNumber("0") }
        binding.btnNum1.setOnClickListener { appendNumber("1") }
        binding.btnNum2.setOnClickListener { appendNumber("2") }
        binding.btnNum3.setOnClickListener { appendNumber("3") }
        binding.btnNum4.setOnClickListener { appendNumber("4") }
        binding.btnNum5.setOnClickListener { appendNumber("5") }
        binding.btnNum6.setOnClickListener { appendNumber("6") }
        binding.btnNum7.setOnClickListener { appendNumber("7") }
        binding.btnNum8.setOnClickListener { appendNumber("8") }
        binding.btnNum9.setOnClickListener { appendNumber("9") }
        binding.btnDot.setOnClickListener { appendNumber(".") }
        binding.btnBackspace.setOnClickListener { backspace() }
    }

    private fun appendNumber(digit: String) {
        if (currentAmount == "0" && digit != ".") {
            currentAmount = digit
        } else if (digit == "." && currentAmount.contains(".")) {
            return
        } else {
            currentAmount += digit
        }
        updateAmountDisplay()
    }

    private fun backspace() {
        if (currentAmount.length > 1) {
            currentAmount = currentAmount.dropLast(1)
        } else {
            currentAmount = "0"
        }
        updateAmountDisplay()
    }

    private fun updateAmountDisplay() {
        binding.tvAmountDisplay.text = currentAmount
    }

    private fun updateUIForType() {
        when (currentType) {
            TransactionType.TRANSFER -> {
                binding.btnCategory.visibility = View.VISIBLE
                binding.tvCategory.text = "Select To Account"
            }
            else -> {
                binding.btnCategory.visibility = View.VISIBLE
                binding.tvCategory.text = "Select Category"
            }
        }
    }

    private fun clearCategorySelection() {
        selectedCategoryId = null
        selectedToAccountId = null
        binding.tvCategory.text = if (currentType == TransactionType.TRANSFER) {
            "Select To Account"
        } else {
            "Select Category"
        }
    }

    private fun updateDateDisplay() {
        val today = Calendar.getInstance()
        val dateText = if (today.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)) {
            "Today"
        } else {
            dateFormat.format(selectedDate.time)
        }
        binding.tvDate.text = dateText
    }

    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDate.set(year, month, day)
                updateDateDisplay()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showAccountPicker() {
        if (accounts.isEmpty()) {
            Toast.makeText(requireContext(), "Please add an account first", Toast.LENGTH_SHORT).show()
            return
        }

        val accountNames = accounts.map { it.name }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Account")
            .setItems(accountNames) { _, which ->
                selectedAccountId = accounts[which].id
                binding.tvAccount.text = accounts[which].name
            }
            .show()
    }

    private fun showToAccountPicker() {
        if (accounts.isEmpty()) {
            Toast.makeText(requireContext(), "Please add an account first", Toast.LENGTH_SHORT).show()
            return
        }

        val accountNames = accounts.map { it.name }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select To Account")
            .setItems(accountNames) { _, which ->
                selectedToAccountId = accounts[which].id
                binding.tvCategory.text = accounts[which].name
            }
            .show()
    }

    private fun showCategoryPicker() {
        val categoryType = when (currentType) {
            TransactionType.EXPENSE -> CategoryType.EXPENSE
            TransactionType.INCOME -> CategoryType.INCOME
            TransactionType.TRANSFER -> return
        }

        val filteredCategories = categories.filter { it.type == categoryType }

        if (filteredCategories.isEmpty()) {
            Toast.makeText(requireContext(), "Please add a category first", Toast.LENGTH_SHORT).show()
            return
        }

        val categoryNames = filteredCategories.map { it.name }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Category")
            .setItems(categoryNames) { _, which ->
                selectedCategoryId = filteredCategories[which].id
                binding.tvCategory.text = filteredCategories[which].name
            }
            .show()
    }

    private fun loadTransactionIfEditing() {
        val transactionId = arguments?.getLong(ARG_TRANSACTION_ID)

        if (transactionId != null && transactionId > 0) {
            lifecycleScope.launch {
                val transaction = viewModel.getTransactionById(transactionId)
                transaction?.let { txn ->
                    editTransaction = txn

                    currentType = txn.type
                    updateTypeSelection()
                    updateUIForType()

                    currentAmount = txn.amount.toString()
                    updateAmountDisplay()

                    val account = accounts.find { it.id == txn.accountId }
                    account?.let {
                        selectedAccountId = it.id
                        binding.tvAccount.text = it.name
                    }

                    if (txn.type == TransactionType.TRANSFER && txn.toAccountId != null) {
                        val toAccount = accounts.find { it.id == txn.toAccountId }
                        toAccount?.let {
                            selectedToAccountId = it.id
                            binding.tvCategory.text = it.name
                        }
                    }

                    if (txn.type != TransactionType.TRANSFER && txn.categoryId != null) {
                        val category = categories.find { it.id == txn.categoryId }
                        category?.let {
                            selectedCategoryId = it.id
                            binding.tvCategory.text = it.name
                        }
                    }

                    selectedDate.timeInMillis = txn.date
                    updateDateDisplay()

                    binding.etNote.setText(txn.note)
                }
            }
        }
    }

    private fun saveTransaction() {
        val amount = currentAmount.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedAccountId == null) {
            Toast.makeText(requireContext(), "Please select an account", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentType == TransactionType.TRANSFER && selectedToAccountId == null) {
            Toast.makeText(requireContext(), "Please select to account", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentType != TransactionType.TRANSFER && selectedCategoryId == null) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        val note = binding.etNote.text.toString().trim()

        val transaction = Transaction(
            accountId = selectedAccountId!!,
            categoryId = if (currentType == TransactionType.TRANSFER) null else selectedCategoryId,
            amount = amount,
            type = currentType,
            date = selectedDate.timeInMillis,
            note = note,
            toAccountId = if (currentType == TransactionType.TRANSFER) selectedToAccountId else null
        )

        if (editTransaction != null) {
            val updatedTransaction = transaction.copy(
                id = editTransaction!!.id,
                createdAt = editTransaction!!.createdAt,
                modifiedAt = System.currentTimeMillis()
            )
            viewModel.updateTransaction(editTransaction!!, updatedTransaction)
            Toast.makeText(requireContext(), "Transaction updated", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insertTransaction(transaction)
            Toast.makeText(requireContext(), "Transaction saved", Toast.LENGTH_SHORT).show()
        }

        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}