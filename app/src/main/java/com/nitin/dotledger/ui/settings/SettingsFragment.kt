package com.nitin.dotledger.ui.settings

import android.Manifest
import android.content.Context
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
import androidx.navigation.fragment.findNavController
import com.nitin.dotledger.R
import com.nitin.dotledger.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val requestSmsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(requireContext(), "SMS permissions granted", Toast.LENGTH_SHORT).show()
            updateSmsPermissionStatus()
        } else {
            Toast.makeText(requireContext(), "SMS permissions denied", Toast.LENGTH_SHORT).show()
            binding.switchAutoParseSms.isChecked = false
            saveSmsAutoParsePreference(false)
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

        loadPreferences()
        setupClickListeners()
        updateSmsPermissionStatus()
    }

    private fun loadPreferences() {
        val prefs = requireContext().getSharedPreferences("dotledger_prefs", Context.MODE_PRIVATE)
        binding.switchAutoParseSms.isChecked = prefs.getBoolean("auto_parse_sms", false)
    }

    private fun setupClickListeners() {
        binding.btnManageCategories.setOnClickListener {
            findNavController().navigate(R.id.categoriesFragment)
        }

        binding.btnExportData.setOnClickListener {
            Toast.makeText(requireContext(), "Export - Coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnImportData.setOnClickListener {
            Toast.makeText(requireContext(), "Import - Coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnManageRecurring.setOnClickListener {
            findNavController().navigate(R.id.recurringTransactionsFragment)
        }

        binding.switchAutoParseSms.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Check if we have SMS permission
                if (hasSmsPermissions()) {
                    saveSmsAutoParsePreference(true)
                } else {
                    // Request permission
                    requestSmsPermissions()
                    binding.switchAutoParseSms.isChecked = false
                }
            } else {
                saveSmsAutoParsePreference(false)
            }
        }

        binding.btnRequestSmsPermission.setOnClickListener {
            if (hasSmsPermissions()) {
                Toast.makeText(requireContext(), "SMS permissions already granted", Toast.LENGTH_SHORT).show()
            } else {
                requestSmsPermissions()
            }
        }
    }

    private fun hasSmsPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.RECEIVE_SMS
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermissions() {
        requestSmsPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS
            )
        )
    }

    private fun updateSmsPermissionStatus() {
        val hasPermission = hasSmsPermissions()
        // Could add visual indicator here if needed
    }

    private fun saveSmsAutoParsePreference(enabled: Boolean) {
        val prefs = requireContext().getSharedPreferences("dotledger_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_parse_sms", enabled).apply()

        if (enabled) {
            Toast.makeText(
                requireContext(),
                "SMS auto-parse enabled. You'll be notified when transactions are detected.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                requireContext(),
                "SMS auto-parse disabled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}