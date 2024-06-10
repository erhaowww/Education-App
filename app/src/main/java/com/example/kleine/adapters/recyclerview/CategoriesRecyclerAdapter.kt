package com.example.kleine.adapters.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kleine.databinding.RecyclerViewCategoryItemBinding
import com.example.kleine.model.Category
import com.example.kleine.model.Material

class CategoriesRecyclerAdapter : RecyclerView.Adapter<CategoriesRecyclerAdapter.CategoriesRecyclerAdapterViewHolder>() {
    inner class CategoriesRecyclerAdapterViewHolder(val binding:RecyclerViewCategoryItemBinding) : RecyclerView.ViewHolder(binding.root)
    private val diffCallback = object : DiffUtil.ItemCallback<Material>() {
        override fun areItemsTheSame(oldItem: Material, newItem: Material): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Material, newItem: Material): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this,diffCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriesRecyclerAdapterViewHolder {
        return CategoriesRecyclerAdapterViewHolder(
            RecyclerViewCategoryItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CategoriesRecyclerAdapterViewHolder, position: Int) {
        val material = differ.currentList[position]
        holder.binding.apply {
            // Assuming you want to display an image related to the difficulty level.
            // You might need to adjust this logic.
            Glide.with(holder.itemView).load(material.imageUrl).into(imgCategory)
            tvCategoryName.text = material.category
        }
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(material)
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

     var onItemClick :((Material)->Unit)?=null
}