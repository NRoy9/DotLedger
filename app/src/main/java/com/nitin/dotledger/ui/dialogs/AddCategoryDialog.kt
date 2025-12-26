package com.nitin.dotledger.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.nitin.dotledger.R
import com.nitin.dotledger.data.entities.Category
import com.nitin.dotledger.data.entities.CategoryType
import com.nitin.dotledger.databinding.DialogAddCategoryBinding
import com.nitin.dotledger.ui.adapters.ColorSelectorAdapter
import com.nitin.dotledger.ui.viewmodel.MainViewModel

class AddCategoryDialog : DialogFragment() {
    private var _binding: DialogAddCategoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private var editCategory: Category? = null
    private var selectedType = CategoryType.EXPENSE
    private var selectedColor = "#FF6384"

    private val availableColors = listOf(
        "#FF6384", "#36A2EB", "#FFCE56", "#4BC0C0",
        "#9966FF", "#FF9F40", "#FF6B6B", "#4ECDC4",
        "#45B7D1", "#FFA07A", "#98D8C8", "#F7DC6F",
        "#BB8FCE", "#85C1E2", "#F8B739", "#52B788"
    )

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"

        fun newInstance(categoryId: Long? = null): AddCategoryDialog {
            val fragment = AddCategoryDialog()
            categoryId?.let {
                val args = Bundle()
                args.putLong(ARG_CATEGORY_ID, it)
                fragment.arguments = args
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupColorSelector()
        loadCategoryIfEditing()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupColorSelector() {
        val colorAdapter = ColorSelectorAdapter(availableColors, selectedColor) { color ->
            selectedColor = color
        }

        binding.rvColors.apply {
            layoutManager = GridLayoutManager(requireContext(), 6)
            adapter = colorAdapter
        }
    }

    private fun loadCategoryIfEditing() {
        val categoryId = arguments?.getLong(ARG_CATEGORY_ID)

        if (categoryId != null && categoryId > 0) {
            binding.tvDialogTitle.text = "EDIT CATEGORY"
            binding.btnSave.text = "UPDATE"

            viewModel.allCategories.observe(viewLifecycleOwner) { categories ->
                val category = categories.find { it.id == categoryId }
                category?.let {
                    editCategory = it
                    binding.etCategoryName.setText(it.name)

                    selectedType = it.type
                    selectedColor = it.colorCode

                    when (it.type) {
                        CategoryType.EXPENSE -> binding.chipExpenseCategory.isChecked = true
                        CategoryType.INCOME -> binding.chipIncomeCategory.isChecked = true
                    }

                    // Update color selector
                    setupColorSelector()

                    // Show delete button only if not a default category
                    if (!it.isDefault) {
                        binding.btnDelete.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.chipGroupCategoryType.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedType = when (checkedIds.firstOrNull()) {
                R.id.chip_income_category -> CategoryType.INCOME
                R.id.chip_expense_category -> CategoryType.EXPENSE
                else -> CategoryType.EXPENSE
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveCategory()
        }

        binding.btnDelete.setOnClickListener {
            deleteCategory()
        }
    }

    private fun saveCategory() {
        val name = binding.etCategoryName.text.toString().trim()

        // Validation
        if (name.isEmpty()) {
            binding.tilCategoryName.error = "Category name is required"
            return
        }

        if (editCategory != null) {
            // Update existing category
            val updatedCategory = editCategory!!.copy(
                name = name,
                type = selectedType,
                colorCode = selectedColor
            )
            viewModel.updateCategory(updatedCategory)
            Toast.makeText(requireContext(), "Category updated", Toast.LENGTH_SHORT).show()
        } else {
            // Create new category
            val newCategory = Category(
                name = name,
                type = selectedType,
                isDefault = false,
                colorCode = selectedColor
            )
            viewModel.insertCategory(newCategory)
            Toast.makeText(requireContext(), "Category created", Toast.LENGTH_SHORT).show()
        }

        dismiss()
    }

    private fun deleteCategory() {
        editCategory?.let {
            if (it.isDefault) {
                Toast.makeText(
                    requireContext(),
                    "Cannot delete default category",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            viewModel.deleteCategory(it)
            Toast.makeText(requireContext(), "Category deleted", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}