package com.nitin.dotledger.ui.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
        "#BB8FCE", "#85C1E2", "#F8B739", "#52B788",
        "#E63946", "#F77F00", "#06FFA5", "#2A9D8F"
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
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

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setupColorSelector()
        setupTypeSelection()
        loadCategoryIfEditing()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
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

    private fun setupTypeSelection() {
        updateTypeUI(CategoryType.EXPENSE)
    }

    private fun loadCategoryIfEditing() {
        val categoryId = arguments?.getLong(ARG_CATEGORY_ID)

        if (categoryId != null && categoryId > 0) {
            binding.tvDialogTitle.text = "EDIT CATEGORY"

            viewModel.allCategories.observe(viewLifecycleOwner) { categories ->
                val category = categories.find { it.id == categoryId }
                category?.let {
                    editCategory = it
                    binding.etCategoryName.setText(it.name)

                    selectedType = it.type
                    selectedColor = it.colorCode

                    updateTypeUI(it.type)
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
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveCategory()
        }

        binding.btnDelete.setOnClickListener {
            deleteCategory()
        }

        binding.btnTypeExpense.setOnClickListener {
            selectedType = CategoryType.EXPENSE
            updateTypeUI(CategoryType.EXPENSE)
        }

        binding.btnTypeIncome.setOnClickListener {
            selectedType = CategoryType.INCOME
            updateTypeUI(CategoryType.INCOME)
        }
    }

    private fun updateTypeUI(type: CategoryType) {
        if (type == CategoryType.EXPENSE) {
            binding.btnTypeExpense.setBackgroundResource(R.drawable.bg_type_selected)
            binding.btnTypeIncome.setBackgroundResource(R.drawable.bg_input_field)
        } else {
            binding.btnTypeExpense.setBackgroundResource(R.drawable.bg_input_field)
            binding.btnTypeIncome.setBackgroundResource(R.drawable.bg_type_selected)
        }
    }

    private fun saveCategory() {
        val name = binding.etCategoryName.text.toString().trim()

        // Validation
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter category name", Toast.LENGTH_SHORT).show()
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

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete ${it.name}?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteCategory(it)
                    Toast.makeText(requireContext(), "Category deleted", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}