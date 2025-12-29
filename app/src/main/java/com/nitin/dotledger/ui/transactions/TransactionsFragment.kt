package com.nitin.dotledger.ui.transactions

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.nitin.dotledger.R
import com.nitin.dotledger.data.entities.Transaction
import com.nitin.dotledger.data.entities.TransactionType
import com.nitin.dotledger.databinding.FragmentTransactionsBinding
import com.nitin.dotledger.ui.adapters.TransactionAdapter
import com.nitin.dotledger.ui.adapters.TransactionWithDetails
import com.nitin.dotledger.ui.dialogs.AddTransactionDialog
import com.nitin.dotledger.ui.dialogs.FilterTransactionsDialog
import com.nitin.dotledger.ui.viewmodel.MainViewModel
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
    private var isSearching = false

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
        setupSearchBar()
        setupClickListeners()
        setupObservers()
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

    private fun setupSearchBar() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                isSearching = query.isNotEmpty()

                if (query.isEmpty()) {
                    // If filter is active, keep using filtered results
                    if (viewModel.hasActiveFilter()) {
                        // Filter will handle it
                    } else {
                        loadTransactions()
                    }
                } else {
                    viewModel.searchTransactions(query)
                }
            }
        })
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
            val selectedType = when (checkedIds.firstOrNull()) {
                R.id.chip_income -> TransactionType.INCOME
                R.id.chip_expense -> TransactionType.EXPENSE
                R.id.chip_transfer -> TransactionType.TRANSFER
                else -> null
            }

            // Quick filter by type only
            val currentFilter = viewModel.currentFilter.value ?: com.nitin.dotledger.data.models.TransactionFilter()
            val types = if (selectedType != null) setOf(selectedType) else emptySet()
            val updatedFilter = currentFilter.copy(types = types)
            viewModel.applyFilter(updatedFilter)
        }

        binding.btnFilter.setOnClickListener {
            FilterTransactionsDialog().show(childFragmentManager, "FilterDialog")
        }

        binding.btnClearFilter.setOnClickListener {
            viewModel.clearFilter()
            binding.chipGroupFilter.clearCheck()
            updateFilterSummary()
        }

        binding.fabAddTransaction.setOnClickListener {
            AddTransactionDialog.newInstance()
                .show(childFragmentManager, "AddTransactionDialog")
        }
    }

    private fun setupObservers() {
        // Observe filter state
        viewModel.currentFilter.observe(viewLifecycleOwner) { _ ->
            updateFilterSummary()
        }

        // Observe filtered transactions
        viewModel.filteredTransactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions != null && (viewModel.hasActiveFilter() || isSearching)) {
                updateTransactionsList(transactions)
            }
        }

        // Observe all accounts and categories for filter summary
        viewModel.allAccounts.observe(viewLifecycleOwner) {
            updateFilterSummary()
        }

        viewModel.allCategories.observe(viewLifecycleOwner) {
            updateFilterSummary()
        }
    }

    private fun updateMonthDisplay() {
        val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvMonthYear.text = format.format(currentCalendar.time)
    }

    private fun loadTransactions() {
        if (viewModel.hasActiveFilter()) {
            // Let filter handle it
            return
        }

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

        // Map to TransactionWithDetails
        val transactionsWithDetails = transactions.mapNotNull { transaction ->
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

    private fun updateFilterSummary() {
        if (viewModel.hasActiveFilter()) {
            val summary = viewModel.getFilterSummary()
            binding.tvActiveFilters.text = summary
            binding.layoutActiveFilters.visibility = View.VISIBLE
        } else {
            binding.layoutActiveFilters.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}