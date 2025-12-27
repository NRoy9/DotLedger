package com.nitin.dotledger.ui.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nitin.dotledger.databinding.ItemTopSpendingBinding
import java.text.NumberFormat
import java.util.*

class TopSpendingAdapter(
    private val items: List<TopSpendingItem>
) : RecyclerView.Adapter<TopSpendingAdapter.TopSpendingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopSpendingViewHolder {
        val binding = ItemTopSpendingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TopSpendingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopSpendingViewHolder, position: Int) {
        holder.bind(items[position], position + 1)
    }

    override fun getItemCount(): Int = items.size.coerceAtMost(5) // Show top 5

    class TopSpendingViewHolder(
        private val binding: ItemTopSpendingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        fun bind(item: TopSpendingItem, rank: Int) {
            binding.apply {
                tvRank.text = rank.toString()
                tvCategoryName.text = item.categoryName
                tvAmount.text = currencyFormat.format(item.amount)
                tvPercentage.text = "${String.format("%.1f", item.percentage)}% of expenses"

                // Set progress
                progressSpending.progress = item.percentage.toInt()

                // Set rank badge color based on position
                val badgeColor = when (rank) {
                    1 -> Color.parseColor("#FF3B30") // Red for #1
                    2 -> Color.parseColor("#FF9500") // Orange for #2
                    3 -> Color.parseColor("#FFCC00") // Yellow for #3
                    else -> Color.parseColor("#8E8E93") // Gray for others
                }

                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.OVAL
                drawable.setColor(badgeColor)
                tvRank.background = drawable
            }
        }
    }
}

data class TopSpendingItem(
    val categoryName: String,
    val amount: Double,
    val percentage: Float,
    val color: String
)