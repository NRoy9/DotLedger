package com.nitin.dotledger.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nitin.dotledger.R
import com.nitin.dotledger.data.entities.Transaction
import com.nitin.dotledger.data.entities.TransactionType
import com.nitin.dotledger.databinding.FragmentTransactionsBinding
import com.nitin.dotledger.ui.adapters.TransactionAdapter
import com.nitin.dotledger.ui.adapters.TransactionWithDetails
import com.nitin.dotledger.ui.dialogs.AddTransactionDialog
import com.nitin.dotledger.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionsFragment : Fragment() {
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var transactionAdapter: TransactionAdapter
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    private var currentCalendar = Calendar.getInstance()
    private var currentFilter: TransactionType? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        updateMonthDisplay()
        loadTransactions()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter { transaction ->
            AddTransactionDialog.newInstance(transaction.id)
                .show(childFragmentManager, "EditTransactionDialog")
        }

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnPrevMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateMonthDisplay()
            loadTransactions()
        }

        binding.btnNextMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateMonthDisplay()
            loadTransactions()
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_income -> TransactionType.INCOME
                R.id.chip_expense -> TransactionType.EXPENSE
                R.id.chip_transfer -> TransactionType.TRANSFER
                else -> null
            }
            loadTransactions()
        }

        binding.fabAddTransaction.setOnClickListener {
            AddTransactionDialog.newInstance()
                .show(childFragmentManager, "AddTransactionDialog")
        }
    }

    private fun updateMonthDisplay() {
        val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvMonthYear.text = format.format(currentCalendar.time)
    }

    private fun loadTransactions() {
        val calendar = currentCalendar.clone() as Calendar

        // Start of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        // End of month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        viewModel.getTransactionsByDateRange(startDate, endDate)
            .observe(viewLifecycleOwner) { transactions ->
                updateTransactionsList(transactions)
            }
    }

    private fun updateTransactionsList(transactions: List<Transaction>) {
        val accounts = viewModel.allAccounts.value ?: emptyList()
        val categories = viewModel.allCategories.value ?: emptyList()

        // Filter by type if selected
        val filteredTransactions = if (currentFilter != null) {
            transactions.filter { it.type == currentFilter }
        } else {
            transactions
        }

        // Show/hide empty state
        if (filteredTransactions.isEmpty()) {
            binding.rvTransactions.visibility = View.GONE
            binding.emptyStateTransactions.visibility = View.VISIBLE
        } else {
            binding.rvTransactions.visibility = View.VISIBLE
            binding.emptyStateTransactions.visibility = View.GONE
        }

        // Map to TransactionWithDetails
        val transactionsWithDetails = filteredTransactions.mapNotNull { transaction ->
            val account = accounts.find { it.id == transaction.accountId }
            val category = transaction.categoryId?.let { categoryId ->
                categories.find { it.id == categoryId }
            }

            account?.let {
                TransactionWithDetails(transaction, it, category)
            }
        }

        transactionAdapter.submitList(transactionsWithDetails)

        // Calculate monthly totals
        val income = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val expense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        binding.tvMonthlyIncome.text = currencyFormat.format(income)
        binding.tvMonthlyExpense.text = currencyFormat.format(expense)
        binding.tvMonthlyNet.text = currencyFormat.format(income - expense)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}