package com.nitin.dotledger.ui.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nitin.dotledger.data.entities.Account
import com.nitin.dotledger.data.entities.AccountType
import com.nitin.dotledger.data.entities.AppSettings
import com.nitin.dotledger.databinding.ItemAccountBinding
import com.nitin.dotledger.utils.CurrencyFormatter

class AccountAdapter(
    private var settings: AppSettings? = null,
    private val onItemClick: (Account) -> Unit
) : ListAdapter<Account, AccountAdapter.AccountViewHolder>(AccountDiffCallback()) {

    fun updateSettings(newSettings: AppSettings?) {
        settings = newSettings
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AccountViewHolder(binding, settings, onItemClick)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(getItem(position), settings)
    }

    class AccountViewHolder(
        private val binding: ItemAccountBinding,
        private var settings: AppSettings?,
        private val onItemClick: (Account) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(account: Account, currentSettings: AppSettings?) {
            settings = currentSettings
            binding.apply {
                tvAccountName.text = account.name
                tvAccountType.text = when (account.type) {
                    AccountType.BANK -> "Bank Account"
                    AccountType.CREDIT_CARD -> "Credit Card"
                    AccountType.WALLET -> "Wallet"
                    AccountType.CASH -> "Cash"
                    AccountType.OTHER -> "Other"
                }

                // Use CurrencyFormatter for balance
                tvAccountBalance.text = CurrencyFormatter.format(account.balance, settings)

                // Set account emoji based on type
                tvAccountIcon.text = when (account.type) {
                    AccountType.BANK -> "🏦"
                    AccountType.CREDIT_CARD -> "💳"
                    AccountType.WALLET -> "👛"
                    AccountType.CASH -> "💵"
                    AccountType.OTHER -> "📊"
                }

                // Set color based on account type
                val indicatorColor = when (account.type) {
                    AccountType.BANK -> Color.parseColor("#34C759")
                    AccountType.CREDIT_CARD -> Color.parseColor("#FF3B30")
                    AccountType.WALLET -> Color.parseColor("#007AFF")
                    AccountType.CASH -> Color.parseColor("#FFC107")
                    AccountType.OTHER -> Color.parseColor("#8E8E93")
                }

                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.OVAL
                drawable.setColor(indicatorColor)
                viewAccountIndicator.background = drawable

                root.setOnClickListener {
                    onItemClick(account)
                }
            }
        }
    }

    class AccountDiffCallback : DiffUtil.ItemCallback<Account>() {
        override fun areItemsTheSame(oldItem: Account, newItem: Account): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Account, newItem: Account): Boolean =
            oldItem == newItem
    }
}