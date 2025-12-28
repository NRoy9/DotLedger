package com.nitin.dotledger.ui.budgets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nitin.dotledger.data.entities.AppSettings
import com.nitin.dotledger.data.entities.BudgetWithSpending
import com.nitin.dotledger.databinding.FragmentBudgetsBinding
import com.nitin.dotledger.ui.adapters.BudgetAdapter
import com.nitin.dotledger.ui.dialogs.AddBudgetDialog
import com.nitin.dotledger.ui.viewmodel.MainViewModel
import com.nitin.dotledger.utils.CurrencyFormatter
import kotlinx.coroutines.launch

class BudgetsFragment : Fragment() {
    private var _binding: FragmentBudgetsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var budgetAdapter: BudgetAdapter
    private var currentSettings: AppSettings? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        budgetAdapter = BudgetAdapter(currentSettings) { budgetWithSpending ->
            AddBudgetDialog.newInstance(budgetWithSpending.budget.id)
                .show(childFragmentManager, "EditBudgetDialog")
        }

        binding.rvBudgets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = budgetAdapter
        }
    }

    private fun setupObservers() {
        // Observe settings
        viewModel.appSettings.observe(viewLifecycleOwner) { settings ->
            currentSettings = settings
            budgetAdapter.updateSettings(settings)
        }

        // Observe budgets and update UI
        viewModel.getCurrentBudgets(System.currentTimeMillis()).observe(viewLifecycleOwner) { budgets ->
            if (budgets.isEmpty()) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.rvBudgets.visibility = View.GONE
                binding.cardOverallSummary.visibility = View.GONE
            } else {
                binding.layoutEmptyState.visibility = View.GONE
                binding.rvBudgets.visibility = View.VISIBLE
                binding.cardOverallSummary.visibility = View.VISIBLE

                loadBudgetsWithSpending(budgets)
            }
        }
    }

    private fun loadBudgetsWithSpending(budgets: List<com.nitin.dotledger.data.entities.Budget>) {
        lifecycleScope.launch {
            val budgetsWithSpending = mutableListOf<BudgetWithSpending>()

            for (budget in budgets) {
                val budgetWithSpending = viewModel.getBudgetWithSpending(budget)
                budgetsWithSpending.add(budgetWithSpending)
            }

            budgetAdapter.submitList(budgetsWithSpending)
            updateSummary(budgetsWithSpending)
        }
    }

    private fun updateSummary(budgetsWithSpending: List<BudgetWithSpending>) {
        val totalBudget = budgetsWithSpending.sumOf { it.budget.amount }
        val totalSpent = budgetsWithSpending.sumOf { it.spent }
        val totalRemaining = totalBudget - totalSpent

        binding.tvTotalBudget.text = CurrencyFormatter.format(totalBudget, currentSettings)
        binding.tvTotalSpent.text = CurrencyFormatter.format(totalSpent, currentSettings)
        binding.tvTotalRemaining.text = CurrencyFormatter.format(totalRemaining, currentSettings)
    }

    private fun setupClickListeners() {
        binding.fabAddBudget.setOnClickListener {
            AddBudgetDialog.newInstance()
                .show(childFragmentManager, "AddBudgetDialog")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}