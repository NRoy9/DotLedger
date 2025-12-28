package com.nitin.dotledger.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nitin.dotledger.data.entities.AppSettings
import com.nitin.dotledger.data.entities.BudgetWithSpending
import com.nitin.dotledger.databinding.ItemBudgetBinding
import com.nitin.dotledger.utils.CurrencyFormatter

class BudgetAdapter(
    private var settings: AppSettings?,
    private val onItemClick: (BudgetWithSpending) -> Unit
) : ListAdapter<BudgetWithSpending, BudgetAdapter.BudgetViewHolder>(BudgetDiffCallback()) {

    fun updateSettings(newSettings: AppSettings?) {
        settings = newSettings
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val binding = ItemBudgetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BudgetViewHolder(binding, settings, onItemClick)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        holder.bind(getItem(position), settings)
    }

    class BudgetViewHolder(
        private val binding: ItemBudgetBinding,
        private var settings: AppSettings?,
        private val onItemClick: (BudgetWithSpending) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BudgetWithSpending, currentSettings: AppSettings?) {
            settings = currentSettings
            binding.apply {
                // Category name or "Overall Budget"
                tvBudgetName.text = item.category?.name ?: "Overall Budget"

                // Budget amount
                tvBudgetAmount.text = CurrencyFormatter.format(item.budget.amount, settings)

                // Spent amount
                tvSpentAmount.text = "Spent: ${CurrencyFormatter.format(item.spent, settings)}"

                // Remaining amount
                val remainingText = if (item.remaining >= 0) {
                    "Left: ${CurrencyFormatter.format(item.remaining, settings)}"
                } else {
                    "Over: ${CurrencyFormatter.format(-item.remaining, settings)}"
                }
                tvRemainingAmount.text = remainingText
                tvRemainingAmount.setTextColor(
                    if (item.remaining >= 0) Color.parseColor("#34C759")
                    else Color.parseColor("#FF3B30")
                )

                // Progress bar
                progressBar.max = 100
                progressBar.progress = item.percentageUsed.toInt().coerceIn(0, 100)

                // Progress bar color based on usage
                val progressColor = when {
                    item.isOverBudget -> Color.parseColor("#FF3B30") // Red
                    item.percentageUsed >= 80 -> Color.parseColor("#FF9F40") // Orange
                    else -> Color.parseColor("#34C759") // Green
                }
                progressBar.progressTintList = android.content.res.ColorStateList.valueOf(progressColor)

                // Percentage text
                tvPercentage.text = "${item.percentageUsed.toInt()}%"
                tvPercentage.setTextColor(progressColor)

                // Warning icon
                if (item.shouldAlert || item.isOverBudget) {
                    tvWarningIcon.visibility = View.VISIBLE
                    tvWarningIcon.text = if (item.isOverBudget) "⚠️" else "⚡"
                } else {
                    tvWarningIcon.visibility = View.GONE
                }

                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }
    }

    class BudgetDiffCallback : DiffUtil.ItemCallback<BudgetWithSpending>() {
        override fun areItemsTheSame(
            oldItem: BudgetWithSpending,
            newItem: BudgetWithSpending
        ): Boolean = oldItem.budget.id == newItem.budget.id

        override fun areContentsTheSame(
            oldItem: BudgetWithSpending,
            newItem: BudgetWithSpending
        ): Boolean = oldItem == newItem
    }
}