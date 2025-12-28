package com.nitin.dotledger.ui.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nitin.dotledger.data.entities.Account
import com.nitin.dotledger.data.entities.AppSettings
import com.nitin.dotledger.data.entities.Category
import com.nitin.dotledger.data.entities.Transaction
import com.nitin.dotledger.data.entities.TransactionType
import com.nitin.dotledger.databinding.ItemTransactionBinding
import com.nitin.dotledger.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private var settings: AppSettings?,
    private val onItemClick: (Transaction) -> Unit
) : ListAdapter<TransactionWithDetails, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    fun updateSettings(newSettings: AppSettings?) {
        settings = newSettings
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding, settings, onItemClick)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position), settings)
    }

    class TransactionViewHolder(
        private val binding: ItemTransactionBinding,
        private var settings: AppSettings?,
        private val onItemClick: (Transaction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun bind(item: TransactionWithDetails, currentSettings: AppSettings?) {
            settings = currentSettings
            binding.apply {
                // Set category name or transfer label
                tvCategory.text = when (item.transaction.type) {
                    TransactionType.TRANSFER -> "Transfer"
                    else -> item.category?.name ?: "Unknown"
                }

                // Set account name
                tvAccount.text = item.account.name

                // Set date - show "Today" if today
                val today = Calendar.getInstance()
                val transactionDate = Calendar.getInstance().apply {
                    timeInMillis = item.transaction.date
                }

                val dateText = if (today.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
                    today.get(Calendar.DAY_OF_YEAR) == transactionDate.get(Calendar.DAY_OF_YEAR)) {
                    "Today"
                } else if (today.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
                    today.get(Calendar.DAY_OF_YEAR) - 1 == transactionDate.get(Calendar.DAY_OF_YEAR)) {
                    "Yesterday"
                } else {
                    dateFormat.format(Date(item.transaction.date))
                }
                tvDate.text = dateText

                // Set amount with color using settings
                val amountText = when (item.transaction.type) {
                    TransactionType.INCOME -> "+${CurrencyFormatter.format(item.transaction.amount, settings)}"
                    TransactionType.EXPENSE -> "-${CurrencyFormatter.format(item.transaction.amount, settings)}"
                    TransactionType.TRANSFER -> CurrencyFormatter.format(item.transaction.amount, settings)
                }
                tvAmount.text = amountText

                val amountColor = when (item.transaction.type) {
                    TransactionType.INCOME -> Color.parseColor("#34C759")
                    TransactionType.EXPENSE -> Color.parseColor("#FF3B30")
                    TransactionType.TRANSFER -> Color.parseColor("#FFFFFF")
                }
                tvAmount.setTextColor(amountColor)

                // Set indicator circle color
                val indicatorColor = when (item.transaction.type) {
                    TransactionType.INCOME -> Color.parseColor("#34C759")
                    TransactionType.EXPENSE -> Color.parseColor("#FF3B30")
                    TransactionType.TRANSFER -> Color.parseColor("#8E8E93")
                }

                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.OVAL
                drawable.setColor(indicatorColor)
                viewTypeIndicator.background = drawable

                // Set category icon emoji
                tvCategoryIcon.text = getCategoryEmoji(item.category?.name ?: "")

                // Click listener
                root.setOnClickListener {
                    onItemClick(item.transaction)
                }
            }
        }

        private fun getCategoryEmoji(categoryName: String): String {
            return when {
                categoryName.contains("Food", ignoreCase = true) ||
                        categoryName.contains("Dining", ignoreCase = true) -> "🍔"
                categoryName.contains("Shopping", ignoreCase = true) -> "🛒"
                categoryName.contains("Transport", ignoreCase = true) -> "🚗"
                categoryName.contains("Entertainment", ignoreCase = true) -> "🎬"
                categoryName.contains("Bills", ignoreCase = true) ||
                        categoryName.contains("Utilities", ignoreCase = true) -> "💡"
                categoryName.contains("Healthcare", ignoreCase = true) -> "🏥"
                categoryName.contains("Investment", ignoreCase = true) -> "💰"
                categoryName.contains("Salary", ignoreCase = true) -> "💵"
                categoryName.contains("Reimbursement", ignoreCase = true) -> "💳"
                else -> "📝"
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionWithDetails>() {
        override fun areItemsTheSame(
            oldItem: TransactionWithDetails,
            newItem: TransactionWithDetails
        ): Boolean = oldItem.transaction.id == newItem.transaction.id

        override fun areContentsTheSame(
            oldItem: TransactionWithDetails,
            newItem: TransactionWithDetails
        ): Boolean = oldItem == newItem
    }
}

data class TransactionWithDetails(
    val transaction: Transaction,
    val account: Account,
    val category: Category?
)