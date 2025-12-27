package com.nitin.dotledger.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nitin.dotledger.data.entities.Account
import com.nitin.dotledger.data.entities.Category
import com.nitin.dotledger.data.entities.Transaction
import com.nitin.dotledger.data.entities.TransactionType
import com.nitin.dotledger.databinding.FragmentDashboardBinding
import com.nitin.dotledger.ui.adapters.TransactionAdapter
import com.nitin.dotledger.ui.adapters.TransactionWithDetails
import com.nitin.dotledger.ui.dialogs.AddTransactionDialog
import com.nitin.dotledger.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var transactionAdapter: TransactionAdapter
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGreeting()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
        binding.tvGreeting.text = greeting
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter { transaction ->
            AddTransactionDialog.newInstance(transaction.id)
                .show(childFragmentManager, "EditTransactionDialog")
        }

        binding.rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    private fun setupObservers() {
        // Observe total balance
        viewModel.totalBalance.observe(viewLifecycleOwner) { balance ->
            binding.tvTotalBalance.text = currencyFormat.format(balance ?: 0.0)
        }

        // Observe total income
        viewModel.getTotalByType(TransactionType.INCOME).observe(viewLifecycleOwner) { income ->
            binding.tvTotalIncome.text = currencyFormat.format(income ?: 0.0)
        }

        // Observe total expense
        viewModel.getTotalByType(TransactionType.EXPENSE).observe(viewLifecycleOwner) { expense ->
            binding.tvTotalExpense.text = currencyFormat.format(expense ?: 0.0)
        }

        // Observe both accounts and transactions before processing
        viewModel.allAccounts.observe(viewLifecycleOwner) { accounts ->
            updateRecentTransactions(accounts, viewModel.allTransactions.value, viewModel.allCategories.value)
        }

        viewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            updateRecentTransactions(viewModel.allAccounts.value, transactions, viewModel.allCategories.value)
        }

        viewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            updateRecentTransactions(viewModel.allAccounts.value, viewModel.allTransactions.value, categories)
        }
    }

    private fun updateRecentTransactions(
        accounts: List<Account>?,
        transactions: List<Transaction>?,
        categories: List<Category>?
    ) {
        if (accounts == null || transactions == null || categories == null) return

        val transactionsWithDetails = transactions.take(5).mapNotNull { transaction ->
            val account = accounts.find { it.id == transaction.accountId }
            val category = transaction.categoryId?.let { categoryId ->
                categories.find { it.id == categoryId }
            }

            account?.let {
                TransactionWithDetails(transaction, it, category)
            }
        }

        // Show/hide empty state
        if (transactionsWithDetails.isEmpty()) {
            binding.rvRecentTransactions.visibility = View.GONE
            binding.emptyStateDashboard.visibility = View.VISIBLE
        } else {
            binding.rvRecentTransactions.visibility = View.VISIBLE
            binding.emptyStateDashboard.visibility = View.GONE
        }

        transactionAdapter.submitList(transactionsWithDetails)
    }

    private fun setupClickListeners() {
        binding.btnAddIncome.setOnClickListener {
            AddTransactionDialog.newInstance(type = TransactionType.INCOME)
                .show(childFragmentManager, "AddIncomeDialog")
        }

        binding.btnAddExpense.setOnClickListener {
            AddTransactionDialog.newInstance(type = TransactionType.EXPENSE)
                .show(childFragmentManager, "AddExpenseDialog")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}