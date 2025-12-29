package com.nitin.dotledger.ui.recurring

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nitin.dotledger.R
import com.nitin.dotledger.data.entities.RecurringTransaction
import com.nitin.dotledger.databinding.FragmentRecurringTransactionsBinding
import com.nitin.dotledger.ui.adapters.RecurringTransactionAdapter
import com.nitin.dotledger.ui.adapters.RecurringWithDetails
import com.nitin.dotledger.ui.dialogs.AddRecurringTransactionDialog
import com.nitin.dotledger.ui.viewmodel.MainViewModel
import com.nitin.dotledger.workers.RecurringTransactionWorker

class RecurringTransactionsFragment : Fragment() {
    private var _binding: FragmentRecurringTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var recurringAdapter: RecurringTransactionAdapter

    private enum class FilterType {
        ALL, ACTIVE, INACTIVE
    }

    private var currentFilter = FilterType.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecurringTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        setupObservers()
    }

    private fun setupRecyclerView() {
        recurringAdapter = RecurringTransactionAdapter(
            onItemClick = { recurring ->
                AddRecurringTransactionDialog.newInstance(recurring.id)
                    .show(childFragmentManager, "EditRecurringDialog")
            },
            onToggleActive = { recurring, isActive ->
                viewModel.toggleRecurringStatus(recurring.id, isActive)
            }
        )

        binding.rvRecurring.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recurringAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddRecurring.setOnClickListener {
            AddRecurringTransactionDialog.newInstance()
                .show(childFragmentManager, "AddRecurringDialog")
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_active -> FilterType.ACTIVE
                R.id.chip_inactive -> FilterType.INACTIVE
                else -> FilterType.ALL
            }
            updateList()
        }

        binding.btnProcessNow.setOnClickListener {
            processDueRecurringTransactions()
        }
    }

    private fun setupObservers() {
        // Observe all recurring transactions
        viewModel.allRecurring.observe(viewLifecycleOwner) { recurringList ->
            updateList()
            updateCounts(recurringList)
        }

        // Observe accounts and categories
        viewModel.allAccounts.observe(viewLifecycleOwner) {
            updateList()
        }

        viewModel.allCategories.observe(viewLifecycleOwner) {
            updateList()
        }
    }

    private fun updateList() {
        val recurringList = viewModel.allRecurring.value ?: emptyList()
        val accounts = viewModel.allAccounts.value ?: emptyList()
        val categories = viewModel.allCategories.value ?: emptyList()

        // Filter based on current filter
        val filteredList = when (currentFilter) {
            FilterType.ACTIVE -> recurringList.filter { it.isActive }
            FilterType.INACTIVE -> recurringList.filter { !it.isActive }
            FilterType.ALL -> recurringList
        }

        // Map to RecurringWithDetails
        val recurringWithDetails = filteredList.mapNotNull { recurring ->
            val account = accounts.find { it.id == recurring.accountId }
            val category = recurring.categoryId?.let { categoryId ->
                categories.find { it.id == categoryId }
            }

            account?.let {
                RecurringWithDetails(recurring, it, category)
            }
        }

        recurringAdapter.submitList(recurringWithDetails)

        // Show/hide empty state
        if (recurringWithDetails.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvRecurring.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvRecurring.visibility = View.VISIBLE
        }
    }

    private fun updateCounts(recurringList: List<RecurringTransaction>) {
        val activeCount = recurringList.count { it.isActive }
        val totalCount = recurringList.size

        binding.tvActiveCount.text = activeCount.toString()
        binding.tvTotalCount.text = totalCount.toString()
    }

    private fun processDueRecurringTransactions() {
        val workRequest = OneTimeWorkRequestBuilder<RecurringTransactionWorker>()
            .build()

        WorkManager.getInstance(requireContext())
            .enqueue(workRequest)

        Toast.makeText(
            requireContext(),
            "Processing due recurring transactions...",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}