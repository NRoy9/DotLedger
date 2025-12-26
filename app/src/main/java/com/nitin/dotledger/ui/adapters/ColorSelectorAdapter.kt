package com.nitin.dotledger.ui.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nitin.dotledger.databinding.ItemColorSelectorBinding

class ColorSelectorAdapter(
    private val colors: List<String>,
    private var selectedColor: String,
    private val onColorSelected: (String) -> Unit
) : RecyclerView.Adapter<ColorSelectorAdapter.ColorViewHolder>() {

    private var selectedPosition = colors.indexOf(selectedColor)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding = ItemColorSelectorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = colors.size

    inner class ColorViewHolder(
        private val binding: ItemColorSelectorBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(colorCode: String, isSelected: Boolean) {
            // Set color
            val color = try {
                Color.parseColor(colorCode)
            } catch (e: Exception) {
                Color.parseColor("#FF0000")
            }

            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            drawable.setColor(color)
            binding.viewColor.background = drawable

            // Show/hide selection border
            binding.viewSelectionBorder.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

            // Click listener
            binding.root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                selectedColor = colorCode

                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                onColorSelected(colorCode)
            }
        }
    }
}