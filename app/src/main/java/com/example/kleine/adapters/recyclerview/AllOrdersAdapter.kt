package com.example.kleine.adapters.recyclerview

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.kleine.R
import com.example.kleine.databinding.RecyclerViewAllOrdersItemBinding
import com.example.kleine.model.Material
import java.text.SimpleDateFormat

class AllOrdersAdapter : RecyclerView.Adapter<AllOrdersAdapter.AllOrdersAdapterViewHolder>() {

    inner class AllOrdersAdapterViewHolder(val binding: RecyclerViewAllOrdersItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    private val diffCallback = object : DiffUtil.ItemCallback<Material>() {
        override fun areItemsTheSame(oldItem: Material, newItem: Material): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Material, newItem: Material): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, diffCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllOrdersAdapterViewHolder {
        return AllOrdersAdapterViewHolder(
            RecyclerViewAllOrdersItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: AllOrdersAdapterViewHolder, position: Int) {
        val material = differ.currentList[position]
        holder.binding.apply {
            productModel = material
            executePendingBindings()
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(material)
        }

        // Adding the long click listener
        holder.itemView.setOnLongClickListener {
            onItemLongClick?.invoke(material)
            true
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    var onItemClick: ((Material) -> Unit)? = null
    var onItemLongClick: ((Material) -> Unit)? = null // Added this line
}
