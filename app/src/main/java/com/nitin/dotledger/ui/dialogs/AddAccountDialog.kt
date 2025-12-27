package com.nitin.dotledger.ui.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.nitin.dotledger.R
import com.nitin.dotledger.data.entities.Account
import com.nitin.dotledger.data.entities.AccountType
import com.nitin.dotledger.databinding.DialogAddAccountBinding
import com.nitin.dotledger.ui.viewmodel.MainViewModel

class AddAccountDialog : DialogFragment() {
    private var _binding: DialogAddAccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private var editAccount: Account? = null

    private var selectedAccountType = AccountType.BANK
    private var currentBalance = "0"

    companion object {
        private const val ARG_ACCOUNT_ID = "account_id"

        fun newInstance(accountId: Long? = null): AddAccountDialog {
            val fragment = AddAccountDialog()
            accountId?.let {
                val args = Bundle()
                args.putLong(ARG_ACCOUNT_ID, it)
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
        _binding = DialogAddAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setupAccountTypeSelection()
        loadAccountIfEditing()
        setupClickListeners()
        updateBalanceDisplay()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private fun setupAccountTypeSelection() {
        // Set initial selection
        updateAccountTypeUI(AccountType.BANK)
    }

    private fun loadAccountIfEditing() {
        val accountId = arguments?.getLong(ARG_ACCOUNT_ID)

        if (accountId != null && accountId > 0) {
            binding.tvDialogTitle.text = "EDIT ACCOUNT"
            binding.btnDelete.visibility = View.VISIBLE

            viewModel.getAccountById(accountId).observe(viewLifecycleOwner) { account ->
                account?.let {
                    editAccount = it
                    binding.etAccountName.setText(it.name)
                    selectedAccountType = it.type
                    currentBalance = it.balance.toString()
                    updateBalanceDisplay()
                    updateAccountTypeUI(it.type)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveAccount()
        }

        binding.btnDelete.setOnClickListener {
            deleteAccount()
        }

        // Account type buttons
        binding.btnTypeBank.setOnClickListener {
            selectedAccountType = AccountType.BANK
            updateAccountTypeUI(AccountType.BANK)
        }

        binding.btnTypeCreditCard.setOnClickListener {
            selectedAccountType = AccountType.CREDIT_CARD
            updateAccountTypeUI(AccountType.CREDIT_CARD)
        }

        binding.btnTypeWallet.setOnClickListener {
            selectedAccountType = AccountType.WALLET
            updateAccountTypeUI(AccountType.WALLET)
        }

        binding.btnTypeCash.setOnClickListener {
            selectedAccountType = AccountType.CASH
            updateAccountTypeUI(AccountType.CASH)
        }

        binding.btnTypeOther.setOnClickListener {
            selectedAccountType = AccountType.OTHER
            updateAccountTypeUI(AccountType.OTHER)
        }

        setupCalculatorButtons()
    }

    private fun updateAccountTypeUI(selectedType: AccountType) {
        // Reset all backgrounds
        binding.btnTypeBank.setBackgroundResource(R.drawable.bg_input_field)
        binding.btnTypeCreditCard.setBackgroundResource(R.drawable.bg_input_field)
        binding.btnTypeWallet.setBackgroundResource(R.drawable.bg_input_field)
        binding.btnTypeCash.setBackgroundResource(R.drawable.bg_input_field)
        binding.btnTypeOther.setBackgroundResource(R.drawable.bg_input_field)

        // Highlight selected
        val selectedView = when (selectedType) {
            AccountType.BANK -> binding.btnTypeBank
            AccountType.CREDIT_CARD -> binding.btnTypeCreditCard
            AccountType.WALLET -> binding.btnTypeWallet
            AccountType.CASH -> binding.btnTypeCash
            AccountType.OTHER -> binding.btnTypeOther
        }
        selectedView.setBackgroundResource(R.drawable.bg_type_selected)

        // Update icon preview
        val (emoji, color) = when (selectedType) {
            AccountType.BANK -> "🏦" to "#34C759"
            AccountType.CREDIT_CARD -> "💳" to "#FF3B30"
            AccountType.WALLET -> "👛" to "#007AFF"
            AccountType.CASH -> "💵" to "#FFC107"
            AccountType.OTHER -> "📊" to "#8E8E93"
        }

        binding.tvAccountIconPreview.text = emoji

        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(Color.parseColor(color))
        binding.viewAccountIconBg.background = drawable
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
        if (currentBalance == "0" && digit != ".") {
            currentBalance = digit
        } else if (digit == "." && currentBalance.contains(".")) {
            return
        } else {
            currentBalance += digit
        }
        updateBalanceDisplay()
    }

    private fun backspace() {
        if (currentBalance.length > 1) {
            currentBalance = currentBalance.dropLast(1)
        } else {
            currentBalance = "0"
        }
        updateBalanceDisplay()
    }

    private fun updateBalanceDisplay() {
        binding.tvBalanceDisplay.text = currentBalance
    }

    private fun saveAccount() {
        val name = binding.etAccountName.text.toString().trim()
        val balance = currentBalance.toDoubleOrNull() ?: 0.0

        // Validation
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter account name", Toast.LENGTH_SHORT).show()
            return
        }

        if (editAccount != null) {
            // Update existing account
            val updatedAccount = editAccount!!.copy(
                name = name,
                type = selectedAccountType,
                balance = balance
            )
            viewModel.updateAccount(updatedAccount)
            Toast.makeText(requireContext(), "Account updated", Toast.LENGTH_SHORT).show()
        } else {
            // Create new account
            val newAccount = Account(
                name = name,
                type = selectedAccountType,
                balance = balance
            )
            viewModel.insertAccount(newAccount)
            Toast.makeText(requireContext(), "Account created", Toast.LENGTH_SHORT).show()
        }

        dismiss()
    }

    private fun deleteAccount() {
        editAccount?.let { account ->
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete ${account.name}? All transactions will also be deleted.")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteAccount(account)
                    Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}