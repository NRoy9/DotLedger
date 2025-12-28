package com.nitin.dotledger.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.nitin.dotledger.R
import com.nitin.dotledger.data.entities.AppSettings
import com.nitin.dotledger.data.entities.Currency
import com.nitin.dotledger.data.entities.CurrencyList
import com.nitin.dotledger.data.entities.NumberFormat
import com.nitin.dotledger.data.entities.TransactionType
import com.nitin.dotledger.databinding.FragmentSettingsBinding
import com.nitin.dotledger.ui.viewmodel.MainViewModel

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private var currentSettings: AppSettings? = null

    // SMS Permission launcher
    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        updateSmsStatus()
        if (isGranted) {
            Toast.makeText(requireContext(), "SMS permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "SMS permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeSettings()
        setupClickListeners()
        updateSmsStatus()
    }

    private fun observeSettings() {
        viewModel.appSettings.observe(viewLifecycleOwner) { settings ->
            settings?.let {
                currentSettings = it
                updateUI(it)
            } ?: run {
                // Initialize default settings if null
                val defaultSettings = AppSettings()
                viewModel.updateSettings(defaultSettings)
            }
        }
    }

    private fun updateUI(settings: AppSettings) {
        // Update Currency display
        binding.tvCurrencyValue.text = "${settings.currencySymbol} ${settings.currency}"

        // Update Number Format display
        val formatText = when (settings.numberFormat) {
            NumberFormat.INDIAN -> "Indian (12,34,567.00)"
            NumberFormat.INTERNATIONAL -> "International (1,234,567.00)"
            NumberFormat.EUROPEAN -> "European (1.234.567,00)"
        }
        binding.tvNumberFormatValue.text = formatText

        // Update Default Transaction Type display
        val typeText = when (settings.defaultTransactionType) {
            TransactionType.INCOME -> "Income"
            TransactionType.EXPENSE -> "Expense"
            TransactionType.TRANSFER -> "Transfer"
        }
        binding.tvDefaultTypeValue.text = typeText
    }

    private fun setupClickListeners() {
        binding.btnCurrency.setOnClickListener {
            showCurrencyPicker()
        }

        binding.btnNumberFormat.setOnClickListener {
            showNumberFormatPicker()
        }

        binding.btnDefaultTransactionType.setOnClickListener {
            showDefaultTransactionTypePicker()
        }

        binding.btnSmsPermission.setOnClickListener {
            requestSmsPermission()
        }

        binding.btnManageCategories.setOnClickListener {
            findNavController().navigate(R.id.categoriesFragment)
        }

        binding.btnManageBudgets.setOnClickListener {
            findNavController().navigate(R.id.budgetsFragment)
        }

        binding.btnExportData.setOnClickListener {
            Toast.makeText(requireContext(), "Export - Coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnImportData.setOnClickListener {
            Toast.makeText(requireContext(), "Import - Coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCurrencyPicker() {
        val currencies = CurrencyList.currencies
        val currencyNames = currencies.map { "${it.symbol} ${it.code} - ${it.name}" }.toTypedArray()

        val currentIndex = currencies.indexOfFirst { it.code == currentSettings?.currency }

        AlertDialog.Builder(requireContext())
            .setTitle("Select Currency")
            .setSingleChoiceItems(currencyNames, currentIndex) { dialog, which ->
                val selectedCurrency = currencies[which]
                updateCurrency(selectedCurrency)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateCurrency(currency: Currency) {
        currentSettings?.let { settings ->
            val updatedSettings = settings.copy(
                currency = currency.code,
                currencySymbol = currency.symbol
            )
            viewModel.updateSettings(updatedSettings)
            Toast.makeText(requireContext(), "Currency updated to ${currency.code}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNumberFormatPicker() {
        val formats = NumberFormat.values()
        val formatNames = arrayOf(
            "Indian (12,34,567.00)",
            "International (1,234,567.00)",
            "European (1.234.567,00)"
        )

        val currentIndex = formats.indexOf(currentSettings?.numberFormat)

        AlertDialog.Builder(requireContext())
            .setTitle("Select Number Format")
            .setSingleChoiceItems(formatNames, currentIndex) { dialog, which ->
                val selectedFormat = formats[which]
                updateNumberFormat(selectedFormat)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateNumberFormat(format: NumberFormat) {
        currentSettings?.let { settings ->
            val updatedSettings = settings.copy(numberFormat = format)
            viewModel.updateSettings(updatedSettings)

            val formatText = when (format) {
                NumberFormat.INDIAN -> "Indian"
                NumberFormat.INTERNATIONAL -> "International"
                NumberFormat.EUROPEAN -> "European"
            }
            Toast.makeText(requireContext(), "Number format updated to $formatText", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDefaultTransactionTypePicker() {
        val types = arrayOf(
            TransactionType.EXPENSE,
            TransactionType.INCOME,
            TransactionType.TRANSFER
        )
        val typeNames = arrayOf("Expense", "Income", "Transfer")

        val currentIndex = types.indexOf(currentSettings?.defaultTransactionType)

        AlertDialog.Builder(requireContext())
            .setTitle("Select Default Transaction Type")
            .setSingleChoiceItems(typeNames, currentIndex) { dialog, which ->
                val selectedType = types[which]
                updateDefaultTransactionType(selectedType)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateDefaultTransactionType(type: TransactionType) {
        currentSettings?.let { settings ->
            val updatedSettings = settings.copy(defaultTransactionType = type)
            viewModel.updateSettings(updatedSettings)

            val typeText = when (type) {
                TransactionType.INCOME -> "Income"
                TransactionType.EXPENSE -> "Expense"
                TransactionType.TRANSFER -> "Transfer"
            }
            Toast.makeText(requireContext(), "Default type updated to $typeText", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSmsStatus() {
        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        binding.tvSmsStatus.text = if (hasPermission) {
            "Enabled - Transactions will be auto-detected"
        } else {
            "Not enabled - Tap to enable"
        }
    }

    private fun requestSmsPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(requireContext(), "SMS permission already granted", Toast.LENGTH_SHORT).show()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("SMS Permission Required")
                    .setMessage("DotLedger needs SMS permission to automatically detect bank transactions from SMS messages. This helps you track expenses automatically.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            else -> {
                smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}