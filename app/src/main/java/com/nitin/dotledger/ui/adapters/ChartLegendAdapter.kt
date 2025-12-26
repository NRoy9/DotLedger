package com.nitin.dotledger.ui.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nitin.dotledger.databinding.ItemChartLegendBinding
import java.text.NumberFormat
import java.util.*

class ChartLegendAdapter(
    private val items: List<LegendItem>
) : RecyclerView.Adapter<ChartLegendAdapter.LegendViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LegendViewHolder {
        val binding = ItemChartLegendBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LegendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LegendViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class LegendViewHolder(
        private val binding: ItemChartLegendBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        fun bind(item: LegendItem) {
            binding.apply {
                tvLegendName.text = item.name
                tvLegendAmount.text = currencyFormat.format(item.amount)
                tvLegendPercentage.text = "${String.format("%.1f", item.percentage)}%"

                // Set color dot
                val color = try {
                    Color.parseColor(item.color)
                } catch (e: Exception) {
                    Color.parseColor("#FF0000")
                }

                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.OVAL
                drawable.setColor(color)
                viewLegendColor.background = drawable
            }
        }
    }
}

data class LegendItem(
    val name: String,
    val amount: Double,
    val percentage: Float,
    val color: String
)