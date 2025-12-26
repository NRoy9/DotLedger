package com.nitin.dotledger.ui.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nitin.dotledger.R
import com.nitin.dotledger.data.entities.CategoryType
import com.nitin.dotledger.databinding.FragmentCategoriesBinding
import com.nitin.dotledger.ui.adapters.CategoryAdapter
import com.nitin.dotledger.ui.dialogs.AddCategoryDialog
import com.nitin.dotledger.ui.viewmodel.MainViewModel

class CategoriesFragment : Fragment() {
    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var categoryAdapter: CategoryAdapter
    private var currentType = CategoryType.EXPENSE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadCategories()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter { category ->
            AddCategoryDialog.newInstance(category.id)
                .show(childFragmentManager, "EditCategoryDialog")
        }

        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            currentType = when (checkedIds.firstOrNull()) {
                R.id.chip_income_categories -> CategoryType.INCOME
                R.id.chip_expense_categories -> CategoryType.EXPENSE
                else -> CategoryType.EXPENSE
            }
            loadCategories()
        }

        binding.fabAddCategory.setOnClickListener {
            AddCategoryDialog.newInstance()
                .show(childFragmentManager, "AddCategoryDialog")
        }
    }

    private fun loadCategories() {
        viewModel.getCategoriesByType(currentType).observe(viewLifecycleOwner) { categories ->
            categoryAdapter.submitList(categories)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}