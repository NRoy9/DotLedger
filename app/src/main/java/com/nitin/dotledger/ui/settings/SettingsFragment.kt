package com.nitin.dotledger.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.nitin.dotledger.R
import com.nitin.dotledger.data.entities.*
import com.nitin.dotledger.databinding.FragmentSettingsBinding
import com.nitin.dotledger.ui.viewmodel.MainViewModel
import com.nitin.dotledger.utils.ExportUtils
import com.nitin.dotledger.utils.ImportUtils
import com.nitin.dotledger.utils.LoadingDialog
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private var currentSettings: AppSettings = AppSettings()

    // SMS Permission Launcher
    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, enable SMS reading
            enableSmsReading(true)
            updateSmsPermissionStatus()
        } else {
            // Permission denied, turn off switch
            binding.switchSmsReading.isChecked = false
            updateSmsPermissionStatus()
            showSmsPermissionDeniedDialog()
        }
    }

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                exportData(uri)
            }
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importData(uri)
            }
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
    }

    private fun observeSettings() {
        viewModel.appSettings.observe(viewLifecycleOwner) { settings ->
            currentSettings = settings ?: AppSettings()
            updateSettingsDisplay()
            updateSmsToggle()
        }
    }

    private fun updateSettingsDisplay() {
        binding.tvCurrentCurrency.text = "${currentSettings.currency} (${currentSettings.currencySymbol})"

        binding.tvCurrentNumberFormat.text = when (currentSettings.numberFormat) {
            NumberFormat.INDIAN -> "Indian (12,34,567)"
            NumberFormat.INTERNATIONAL -> "International (1,234,567)"
            NumberFormat.EUROPEAN -> "European (1.234.567)"
        }

        binding.tvDefaultTransactionType.text = when (currentSettings.defaultTransactionType) {
            TransactionType.INCOME -> "Income"
            TransactionType.EXPENSE -> "Expense"
            TransactionType.TRANSFER -> "Transfer"
        }
    }

    private fun updateSmsToggle() {
        binding.switchSmsReading.isChecked = currentSettings.enableSmsReading
        updateSmsPermissionStatus()
    }

    private fun updateSmsPermissionStatus() {
        val hasPermission = checkSmsPermission()
        val isEnabled = currentSettings.enableSmsReading

        if (isEnabled && !hasPermission) {
            binding.layoutSmsPermissionStatus.visibility = View.VISIBLE
            binding.tvSmsPermissionStatus.text = "⚠️ SMS permission required. Tap to grant."
            binding.layoutSmsPermissionStatus.setOnClickListener {
                requestSmsPermission()
            }
        } else if (isEnabled && hasPermission) {
            binding.layoutSmsPermissionStatus.visibility = View.VISIBLE
            binding.tvSmsPermissionStatus.text = "✓ SMS reading is active"
            binding.layoutSmsPermissionStatus.setOnClickListener(null)
        } else {
            binding.layoutSmsPermissionStatus.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnCurrency.setOnClickListener {
            showCurrencyPicker()
        }

        binding.btnNumberFormat.setOnClickListener {
            showNumberFormatPicker()
        }

        binding.btnDefaultTransactionType.setOnClickListener {
            showTransactionTypePicker()
        }

        binding.btnManageCategories.setOnClickListener {
            findNavController().navigate(R.id.categoriesFragment)
        }

        // SMS Reading Toggle
        binding.switchSmsReading.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Check permission first
                if (checkSmsPermission()) {
                    enableSmsReading(true)
                } else {
                    // Request permission
                    requestSmsPermission()
                }
            } else {
                enableSmsReading(false)
            }
        }

        binding.btnExportData.setOnClickListener {
            initiateExport()
        }

        binding.btnImportData.setOnClickListener {
            initiateImport()
        }
    }

    private fun checkSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.RECEIVE_SMS
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS)) {
            // Show explanation
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("SMS Permission Required")
                .setMessage("DotLedger needs SMS permission to automatically read bank transaction messages and create transactions. This helps you track expenses without manual entry.")
                .setPositiveButton("Grant Permission") { _, _ ->
                    smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    binding.switchSmsReading.isChecked = false
                }
                .show()
        } else {
            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    private fun showSmsPermissionDeniedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Permission Denied")
            .setMessage("SMS permission is required for auto-reading bank messages. You can enable it in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireContext().packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun enableSmsReading(enable: Boolean) {
        currentSettings = currentSettings.copy(enableSmsReading = enable)
        viewModel.updateSettings(currentSettings)

        val message = if (enable) {
            "✓ SMS reading enabled. Bank SMS will be auto-processed."
        } else {
            "SMS reading disabled"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showCurrencyPicker() {
        val currencies = CurrencyList.currencies
        val currencyNames = currencies.map { "${it.name} (${it.symbol})" }.toTypedArray()

        val currentIndex = currencies.indexOfFirst { it.code == currentSettings.currency }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Currency")
            .setSingleChoiceItems(currencyNames, currentIndex) { dialog, which ->
                val selected = currencies[which]
                updateCurrency(selected)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNumberFormatPicker() {
        val formats = arrayOf(
            "Indian (12,34,567)",
            "International (1,234,567)",
            "European (1.234.567)"
        )

        val currentIndex = when (currentSettings.numberFormat) {
            NumberFormat.INDIAN -> 0
            NumberFormat.INTERNATIONAL -> 1
            NumberFormat.EUROPEAN -> 2
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Number Format")
            .setSingleChoiceItems(formats, currentIndex) { dialog, which ->
                val format = when (which) {
                    0 -> NumberFormat.INDIAN
                    1 -> NumberFormat.INTERNATIONAL
                    else -> NumberFormat.EUROPEAN
                }
                updateNumberFormat(format)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTransactionTypePicker() {
        val types = arrayOf("Income", "Expense", "Transfer")

        val currentIndex = when (currentSettings.defaultTransactionType) {
            TransactionType.INCOME -> 0
            TransactionType.EXPENSE -> 1
            TransactionType.TRANSFER -> 2
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Default Transaction Type")
            .setSingleChoiceItems(types, currentIndex) { dialog, which ->
                val type = when (which) {
                    0 -> TransactionType.INCOME
                    1 -> TransactionType.EXPENSE
                    else -> TransactionType.TRANSFER
                }
                updateDefaultTransactionType(type)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateCurrency(currency: Currency) {
        currentSettings = currentSettings.copy(
            currency = currency.code,
            currencySymbol = currency.symbol
        )
        viewModel.updateSettings(currentSettings)
        Toast.makeText(requireContext(), "Currency updated to ${currency.code}", Toast.LENGTH_SHORT).show()
    }

    private fun updateNumberFormat(format: NumberFormat) {
        currentSettings = currentSettings.copy(numberFormat = format)
        viewModel.updateSettings(currentSettings)
        Toast.makeText(requireContext(), "Number format updated", Toast.LENGTH_SHORT).show()
    }

    private fun updateDefaultTransactionType(type: TransactionType) {
        currentSettings = currentSettings.copy(defaultTransactionType = type)
        viewModel.updateSettings(currentSettings)
        Toast.makeText(requireContext(), "Default transaction type updated", Toast.LENGTH_SHORT).show()
    }

    private fun initiateExport() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "dotledger_export_${System.currentTimeMillis()}.csv")
        }
        exportLauncher.launch(intent)
    }

    private fun exportData(uri: Uri) {
        val loadingDialog = LoadingDialog(requireContext())

        lifecycleScope.launch {
            try {
                loadingDialog.show("Exporting data...")

                val accounts = viewModel.allAccounts.value ?: emptyList()
                val categories = viewModel.allCategories.value ?: emptyList()
                val transactions = viewModel.allTransactions.value ?: emptyList()

                ExportUtils.exportToCSV(
                    requireContext(),
                    uri,
                    accounts,
                    categories,
                    transactions
                )

                loadingDialog.dismiss()

                Toast.makeText(
                    requireContext(),
                    "✓ Export successful!\n${accounts.size} accounts, ${categories.size} categories, ${transactions.size} transactions",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                loadingDialog.dismiss()

                Toast.makeText(
                    requireContext(),
                    "✗ Export failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
        }
    }

    private fun initiateImport() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Import Data")
            .setMessage("This will replace all existing data. Are you sure you want to continue?")
            .setPositiveButton("Import") { _, _ ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/*"
                }
                importLauncher.launch(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun importData(uri: Uri) {
        val loadingDialog = LoadingDialog(requireContext())

        lifecycleScope.launch {
            try {
                loadingDialog.show("Importing data...")

                val result = ImportUtils.importFromCSV(requireContext(), uri)

                loadingDialog.updateMessage("Clearing old data...")

                // Clear existing data (except default categories)
                viewModel.allTransactions.value?.forEach { viewModel.deleteTransaction(it) }
                viewModel.allAccounts.value?.forEach { viewModel.deleteAccount(it) }
                viewModel.allCategories.value?.filter { !it.isDefault }?.forEach {
                    viewModel.deleteCategory(it)
                }

                loadingDialog.updateMessage("Importing new data...")

                // Import new data
                result.accounts.forEach { viewModel.insertAccount(it) }
                result.categories.filter { !it.isDefault }.forEach { viewModel.insertCategory(it) }
                result.transactions.forEach { viewModel.insertTransaction(it) }

                loadingDialog.dismiss()

                Toast.makeText(
                    requireContext(),
                    "✓ Import successful!\n${result.accounts.size} accounts, ${result.categories.size} categories, ${result.transactions.size} transactions",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                loadingDialog.dismiss()

                Toast.makeText(
                    requireContext(),
                    "✗ Import failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}