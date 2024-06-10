package com.example.kleine.adapters.recyclerview

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kleine.databinding.CartItemBinding
import com.example.kleine.model.CartProduct
import com.example.kleine.model.CourseDocument
import com.example.kleine.util.Constants.Companion.CART_FLAG

class CartRecyclerAdapter(
    private val itemFlag: String = CART_FLAG
) : RecyclerView.Adapter<CartRecyclerAdapter.CartRecyclerAdapterViewHolder>() {

    var onPlusClick: ((CartProduct) -> Unit)? = null
    var onMinusesClick: ((CartProduct) -> Unit)? = null
    var onItemClick: ((CartProduct) -> Unit)? = null
    var onDocumentDownloadClick: ((String) -> Unit)? = null // Lambda to handle document download

    private val courseDocuments = mutableListOf<CourseDocument>()


    fun submitList(list: List<CourseDocument>) {
        courseDocuments.clear()
        courseDocuments.addAll(list)
        notifyDataSetChanged()
        Log.d(TAG, "List submitted with size: ${courseDocuments.size}")
    }


    inner class CartRecyclerAdapterViewHolder(val binding: CartItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    private val diffCallBack = object : DiffUtil.ItemCallback<CartProduct>() {
        override fun areItemsTheSame(oldItem: CartProduct, newItem: CartProduct): Boolean {
            return oldItem.id == newItem.id && oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: CartProduct, newItem: CartProduct): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, diffCallBack)


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CartRecyclerAdapterViewHolder {
        return CartRecyclerAdapterViewHolder(
            CartItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }


    override fun onBindViewHolder(holder: CartRecyclerAdapterViewHolder, position: Int) {
        val courseDocument = courseDocuments.getOrNull(position)

        // Set CourseDocument to the item's binding
        holder.binding.courseDocument = courseDocument

        // Set the click listener for tvQuantity
        holder.binding.tvQuantity.setOnClickListener {
            courseDocument?.let {
                onDocumentDownloadClick?.invoke(it.documentUrl)
            } ?: run {
                // Log an error or handle the case where courseDocument is null
                Log.e(TAG, "CourseDocument is null")
            }
        }
    }




    override fun getItemCount(): Int {
        val itemCount = courseDocuments.size
        Log.d(TAG, "Item count: $itemCount")
        return itemCount
    }




    companion object {
        private const val TAG = "CartRecyclerAdapter"
    }

}