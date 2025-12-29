package com.nitin.dotledger.ui.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nitin.dotledger.data.entities.Account
import com.nitin.dotledger.data.entities.Category
import com.nitin.dotledger.data.entities.RecurringTransaction
import com.nitin.dotledger.data.entities.TransactionType
import com.nitin.dotledger.databinding.ItemRecurringTransactionBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class RecurringTransactionAdapter(
    private val onItemClick: (RecurringTransaction) -> Unit,
    private val onToggleActive: (RecurringTransaction, Boolean) -> Unit
) : ListAdapter<RecurringWithDetails, RecurringTransactionAdapter.RecurringViewHolder>(RecurringDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecurringViewHolder {
        val binding = ItemRecurringTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecurringViewHolder(binding, onItemClick, onToggleActive)
    }

    override fun onBindViewHolder(holder: RecurringViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecurringViewHolder(
        private val binding: ItemRecurringTransactionBinding,
        private val onItemClick: (RecurringTransaction) -> Unit,
        private val onToggleActive: (RecurringTransaction, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun bind(item: RecurringWithDetails) {
            binding.apply {
                tvName.text = item.recurring.name
                tvAmount.text = currencyFormat.format(item.recurring.amount)
                tvFrequency.text = item.recurring.frequency.displayName
                tvAccount.text = item.account.name
                tvCategory.text = item.category?.name ?: "Transfer"
                tvNextOccurrence.text = "Next: ${dateFormat.format(Date(item.recurring.nextOccurrence))}"

                // Set amount color based on type
                val amountColor = when (item.recurring.type) {
                    TransactionType.INCOME -> Color.parseColor("#34C759")
                    TransactionType.EXPENSE -> Color.parseColor("#FF3B30")
                    TransactionType.TRANSFER -> Color.parseColor("#8E8E93")
                }
                tvAmount.setTextColor(amountColor)

                // Set indicator color
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.OVAL
                drawable.setColor(amountColor)
                viewTypeIndicator.background = drawable

                // Set type icon
                tvTypeIcon.text = when (item.recurring.type) {
                    TransactionType.INCOME -> "↑"
                    TransactionType.EXPENSE -> "↓"
                    TransactionType.TRANSFER -> "→"
                }

                // Set active switch
                switchActive.isChecked = item.recurring.isActive
                switchActive.setOnCheckedChangeListener { _, isChecked ->
                    onToggleActive(item.recurring, isChecked)
                }

                // Show end date if available
                if (item.recurring.endDate != null) {
                    tvEndDate.text = "Until: ${dateFormat.format(Date(item.recurring.endDate))}"
                    tvEndDate.visibility = android.view.View.VISIBLE
                } else {
                    tvEndDate.text = "No end date"
                    tvEndDate.visibility = android.view.View.VISIBLE
                }

                // Set alpha for inactive items
                root.alpha = if (item.recurring.isActive) 1.0f else 0.5f

                root.setOnClickListener {
                    onItemClick(item.recurring)
                }
            }
        }
    }

    class RecurringDiffCallback : DiffUtil.ItemCallback<RecurringWithDetails>() {
        override fun areItemsTheSame(
            oldItem: RecurringWithDetails,
            newItem: RecurringWithDetails
        ): Boolean = oldItem.recurring.id == newItem.recurring.id

        override fun areContentsTheSame(
            oldItem: RecurringWithDetails,
            newItem: RecurringWithDetails
        ): Boolean = oldItem == newItem
    }
}

data class RecurringWithDetails(
    val recurring: RecurringTransaction,
    val account: Account,
    val category: Category?
)