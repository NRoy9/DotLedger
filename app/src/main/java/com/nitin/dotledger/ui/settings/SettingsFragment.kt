package com.nitin.dotledger.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nitin.dotledger.R
import com.nitin.dotledger.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

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
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnManageCategories.setOnClickListener {
            findNavController().navigate(R.id.categoriesFragment)
        }

        binding.btnExportData.setOnClickListener {
            // TODO: Implement export functionality
            Toast.makeText(requireContext(), "Export - Coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnImportData.setOnClickListener {
            // TODO: Implement import functionality
            Toast.makeText(requireContext(), "Import - Coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}