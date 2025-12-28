package com.nitin.dotledger.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.nitin.dotledger.data.entities.AppSettings
import com.nitin.dotledger.databinding.FragmentAccountsBinding
import com.nitin.dotledger.ui.adapters.AccountAdapter
import com.nitin.dotledger.ui.dialogs.AddAccountDialog
import com.nitin.dotledger.ui.viewmodel.MainViewModel
import com.nitin.dotledger.utils.CurrencyFormatter

class AccountsFragment : Fragment() {
    private var _binding: FragmentAccountsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var accountAdapter: AccountAdapter
    private var currentSettings: AppSettings? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        accountAdapter = AccountAdapter(currentSettings) { account ->
            AddAccountDialog.newInstance(account.id)
                .show(childFragmentManager, "EditAccountDialog")
        }

        binding.rvAccounts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = accountAdapter
        }
    }

    private fun setupObservers() {
        // Observe settings
        viewModel.appSettings.observe(viewLifecycleOwner) { settings ->
            currentSettings = settings
            accountAdapter.updateSettings(settings)
            updateTotalBalance()
        }

        viewModel.allAccounts.observe(viewLifecycleOwner) { accounts ->
            accountAdapter.submitList(accounts)
            updateTotalBalance()
        }
    }

    private fun updateTotalBalance() {
        val totalBalance = viewModel.allAccounts.value?.sumOf { it.balance } ?: 0.0
        binding.tvTotalAccountsBalance.text = CurrencyFormatter.format(totalBalance, currentSettings)
    }

    private fun setupClickListeners() {
        binding.fabAddAccount.setOnClickListener {
            AddAccountDialog.newInstance()
                .show(childFragmentManager, "AddAccountDialog")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}