package com.nitin.dotledger.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.nitin.dotledger.data.entities.TransactionType
import com.nitin.dotledger.databinding.FragmentStatisticsBinding
import com.nitin.dotledger.ui.adapters.ChartLegendAdapter
import com.nitin.dotledger.ui.adapters.LegendItem
import com.nitin.dotledger.ui.adapters.TopSpendingAdapter
import com.nitin.dotledger.ui.adapters.TopSpendingItem
import com.nitin.dotledger.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class StatisticsFragment : Fragment() {
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private var currentCalendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCharts()
        setupClickListeners()
        updateMonthDisplay()
        loadStatistics()
    }

    private fun setupCharts() {
        setupPieChart(binding.chartExpense)
        setupPieChart(binding.chartIncome)
    }

    private fun setupPieChart(chart: PieChart) {
        chart.apply {
            setUsePercentValues(false)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)

            dragDecelerationFrictionCoef = 0.95f

            isDrawHoleEnabled = true
            setHoleColor(Color.parseColor("#1C1C1E"))
            holeRadius = 65f
            transparentCircleRadius = 70f

            setDrawCenterText(true)
            centerTextRadiusPercent = 80f
            setCenterTextColor(Color.WHITE)
            setCenterTextSize(18f)

            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true

            legend.isEnabled = false

            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(11f)
            setDrawEntryLabels(false)

            animateY(1200, Easing.EaseInOutQuad)
        }
    }

    private fun setupClickListeners() {
        binding.btnPrevMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateMonthDisplay()
            loadStatistics()
        }

        binding.btnNextMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateMonthDisplay()
            loadStatistics()
        }
    }

    private fun updateMonthDisplay() {
        val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvMonthYear.text = format.format(currentCalendar.time)
    }

    private fun loadStatistics() {
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

        lifecycleScope.launch {
            // Get category totals
            val expenseTotals = viewModel.getCategoryTotals(TransactionType.EXPENSE, startDate, endDate)
            val incomeTotals = viewModel.getCategoryTotals(TransactionType.INCOME, startDate, endDate)

            // Calculate totals
            val totalExpense = expenseTotals.sumOf { it.total }
            val totalIncome = incomeTotals.sumOf { it.total }
            val netFlow = totalIncome - totalExpense

            // Get transaction count
            val transactions = viewModel.getTransactionsByDateRange(startDate, endDate).value ?: emptyList()
            val transactionCount = transactions.size

            // Update summary
            binding.tvTotalIncome.text = currencyFormat.format(totalIncome)
            binding.tvTotalExpense.text = currencyFormat.format(totalExpense)
            binding.tvNetFlow.text = currencyFormat.format(netFlow)
            binding.tvTransactionCount.text = transactionCount.toString()

            // Set net flow color
            binding.tvNetFlow.setTextColor(when {
                netFlow > 0 -> Color.parseColor("#34C759")
                netFlow < 0 -> Color.parseColor("#FF3B30")
                else -> Color.WHITE
            })

            // Update expense chart
            updateChart(
                binding.chartExpense,
                binding.rvExpenseLegend,
                binding.emptyStateExpense,
                expenseTotals,
                totalExpense,
                TransactionType.EXPENSE
            )

            // Update income chart
            updateChart(
                binding.chartIncome,
                binding.rvIncomeLegend,
                binding.emptyStateIncome,
                incomeTotals,
                totalIncome,
                TransactionType.INCOME
            )

            // Update top spending
            updateTopSpending(expenseTotals, totalExpense)
        }
    }

    private suspend fun updateChart(
        chart: PieChart,
        legendRecyclerView: androidx.recyclerview.widget.RecyclerView,
        emptyStateView: View,
        categoryTotals: List<com.nitin.dotledger.data.dao.CategoryTotal>,
        total: Double,
        type: TransactionType
    ) {
        if (categoryTotals.isEmpty() || total == 0.0) {
            chart.visibility = View.GONE
            legendRecyclerView.visibility = View.GONE
            emptyStateView.visibility = View.VISIBLE
            return
        }

        chart.visibility = View.VISIBLE
        legendRecyclerView.visibility = View.VISIBLE
        emptyStateView.visibility = View.GONE

        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()
        val legendItems = mutableListOf<LegendItem>()

        val allCategories = viewModel.allCategories.value ?: emptyList()

        for (categoryTotal in categoryTotals) {
            val category = categoryTotal.categoryId?.let { catId ->
                allCategories.find { it.id == catId }
            }

            val categoryName = category?.name ?: "Unknown"
            val categoryColor = category?.colorCode ?: "#FF0000"
            val percentage = ((categoryTotal.total / total) * 100).toFloat()

            entries.add(PieEntry(categoryTotal.total.toFloat(), categoryName))

            // Parse color safely
            try {
                colors.add(Color.parseColor(categoryColor))
            } catch (e: Exception) {
                colors.add(Color.parseColor("#FF0000"))
            }

            legendItems.add(
                LegendItem(
                    name = categoryName,
                    amount = categoryTotal.total,
                    percentage = percentage,
                    color = categoryColor
                )
            )
        }

        // Sort legend by amount descending
        legendItems.sortByDescending { it.amount }

        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)
            sliceSpace = 2f
            selectionShift = 8f
            valueTextColor = Color.WHITE
            valueTextSize = 12f
            setDrawValues(false)
        }

        val data = PieData(dataSet)
        chart.data = data

        // Set center text
        val centerText = when (type) {
            TransactionType.EXPENSE -> "Total\nExpense"
            TransactionType.INCOME -> "Total\nIncome"
            else -> ""
        }
        chart.centerText = centerText

        chart.highlightValues(null)
        chart.invalidate()

        // Update legend
        legendRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ChartLegendAdapter(legendItems)
        }
    }

    private fun updateTopSpending(
        expenseTotals: List<com.nitin.dotledger.data.dao.CategoryTotal>,
        totalExpense: Double
    ) {
        if (expenseTotals.isEmpty() || totalExpense == 0.0) {
            binding.rvTopSpending.visibility = View.GONE
            return
        }

        binding.rvTopSpending.visibility = View.VISIBLE

        val allCategories = viewModel.allCategories.value ?: emptyList()

        val topItems = expenseTotals
            .sortedByDescending { it.total }
            .take(5)
            .map { categoryTotal ->
                val category = categoryTotal.categoryId?.let { catId ->
                    allCategories.find { it.id == catId }
                }

                TopSpendingItem(
                    categoryName = category?.name ?: "Unknown",
                    amount = categoryTotal.total,
                    percentage = ((categoryTotal.total / totalExpense) * 100).toFloat(),
                    color = category?.colorCode ?: "#FF0000"
                )
            }

        binding.rvTopSpending.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = TopSpendingAdapter(topItems)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}