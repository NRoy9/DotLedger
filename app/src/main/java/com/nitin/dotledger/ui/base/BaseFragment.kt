package com.nitin.dotledger.ui.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.nitin.dotledger.data.entities.AppSettings
import com.nitin.dotledger.ui.viewmodel.MainViewModel
import com.nitin.dotledger.utils.CurrencyFormatter

open class BaseFragment : Fragment() {

    protected val viewModel: MainViewModel by activityViewModels()
    protected var currentSettings: AppSettings = AppSettings()

    /**
     * Format currency based on current settings
     */
    protected fun formatCurrency(amount: Double): String {
        return CurrencyFormatter.format(amount, currentSettings)
    }

    /**
     * Get currency symbol
     */
    protected fun getCurrencySymbol(): String {
        return currentSettings.currencySymbol
    }

    /**
     * Setup settings observer - call this in onViewCreated
     */
    protected fun observeAppSettings(onSettingsChanged: ((AppSettings) -> Unit)? = null) {
        viewModel.appSettings.observe(viewLifecycleOwner) { settings ->
            currentSettings = settings ?: AppSettings()
            onSettingsChanged?.invoke(currentSettings)
        }
    }
}