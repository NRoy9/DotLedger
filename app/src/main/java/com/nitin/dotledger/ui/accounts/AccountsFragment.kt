package com.nitin.dotledger.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.nitin.dotledger.databinding.FragmentAccountsBinding
import com.nitin.dotledger.ui.adapters.AccountAdapter
import com.nitin.dotledger.ui.dialogs.AddAccountDialog
import com.nitin.dotledger.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.*

class AccountsFragment : Fragment() {
    private var _binding: FragmentAccountsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var accountAdapter: AccountAdapter
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

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
        accountAdapter = AccountAdapter { account ->
            AddAccountDialog.newInstance(account.id)
                .show(childFragmentManager, "EditAccountDialog")
        }

        binding.rvAccounts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = accountAdapter
        }
    }

    private fun setupObservers() {
        viewModel.allAccounts.observe(viewLifecycleOwner) { accounts ->
            accountAdapter.submitList(accounts)

            // Show/hide empty state
            if (accounts.isEmpty()) {
                binding.rvAccounts.visibility = View.GONE
                binding.emptyStateAccounts.visibility = View.VISIBLE
            } else {
                binding.rvAccounts.visibility = View.VISIBLE
                binding.emptyStateAccounts.visibility = View.GONE
            }

            // Calculate total balance
            val totalBalance = accounts.sumOf { it.balance }
            binding.tvTotalAccountsBalance.text = currencyFormat.format(totalBalance)
        }
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