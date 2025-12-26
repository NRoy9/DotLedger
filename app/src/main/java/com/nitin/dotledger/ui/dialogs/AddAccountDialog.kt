package com.nitin.dotledger.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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

        setupAccountTypeDropdown()
        loadAccountIfEditing()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupAccountTypeDropdown() {
        val accountTypes = AccountType.values().map {
            it.name.replace("_", " ")
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            accountTypes
        )

        binding.etAccountType.setAdapter(adapter)
        binding.etAccountType.setText(accountTypes[0], false)
    }

    private fun loadAccountIfEditing() {
        val accountId = arguments?.getLong(ARG_ACCOUNT_ID)

        if (accountId != null && accountId > 0) {
            // Editing mode
            binding.tvDialogTitle.text = "EDIT ACCOUNT"
            binding.btnSave.text = "UPDATE"
            binding.btnDelete.visibility = View.VISIBLE

            viewModel.getAccountById(accountId).observe(viewLifecycleOwner) { account ->
                account?.let {
                    editAccount = it
                    binding.etAccountName.setText(it.name)
                    binding.etAccountType.setText(
                        it.type.name.replace("_", " "),
                        false
                    )
                    binding.etInitialBalance.setText(it.balance.toString())
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveAccount()
        }

        binding.btnDelete.setOnClickListener {
            deleteAccount()
        }
    }

    private fun saveAccount() {
        val name = binding.etAccountName.text.toString().trim()
        val typeString = binding.etAccountType.text.toString().trim()
        val balanceString = binding.etInitialBalance.text.toString().trim()

        // Validation
        if (name.isEmpty()) {
            binding.tilAccountName.error = "Account name is required"
            return
        }

        if (typeString.isEmpty()) {
            binding.tilAccountType.error = "Account type is required"
            return
        }

        val balance = balanceString.toDoubleOrNull() ?: 0.0
        val type = AccountType.valueOf(typeString.replace(" ", "_"))

        if (editAccount != null) {
            // Update existing account
            val updatedAccount = editAccount!!.copy(
                name = name,
                type = type,
                balance = balance
            )
            viewModel.updateAccount(updatedAccount)
            Toast.makeText(requireContext(), "Account updated", Toast.LENGTH_SHORT).show()
        } else {
            // Create new account
            val newAccount = Account(
                name = name,
                type = type,
                balance = balance
            )
            viewModel.insertAccount(newAccount)
            Toast.makeText(requireContext(), "Account created", Toast.LENGTH_SHORT).show()
        }

        dismiss()
    }

    private fun deleteAccount() {
        editAccount?.let {
            viewModel.deleteAccount(it)
            Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}