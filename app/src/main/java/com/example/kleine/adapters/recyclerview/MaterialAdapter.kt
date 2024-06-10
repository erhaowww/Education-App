package com.example.kleine.adapters.recyclerview

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kleine.R
import com.example.kleine.databinding.ProductLayoutRowBinding
import com.example.kleine.fragments.shopping.HomeFragmentDirections
import com.example.kleine.model.Material
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class MaterialAdapter : RecyclerView.Adapter<MaterialAdapter.MaterialViewHolder>() {

    var onItemClick: ((Material) -> Unit)? = null

    inner class MaterialViewHolder(val binding: ProductLayoutRowBinding) : RecyclerView.ViewHolder(binding.root) {

    }

    private val diffCallback = object : DiffUtil.ItemCallback<Material>() {
        override fun areItemsTheSame(oldItem: Material, newItem: Material): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Material, newItem: Material): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, diffCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialViewHolder {
        Log.d("MaterialAdapter", "onCreateViewHolder called")
        return MaterialViewHolder(
            ProductLayoutRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    // Define a helper function for debounced clicks
    fun View.setDebouncedOnClickListener(debounceTime: Long = 500L, onClick: (view: View) -> Unit) {
        var lastClickTime = 0L
        this.setOnClickListener {
            if (System.currentTimeMillis() - lastClickTime >= debounceTime) {
                onClick(it)
                lastClickTime = System.currentTimeMillis()
            }
        }
    }

    override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {
        Log.d("MaterialAdapter", "onBindViewHolder called for position $position")

        val material = differ.currentList[position]
        holder.binding.apply {
            productModel = material

            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference

            // Set the color and clickability based on the status
            if (material.status == "Available") {
                cardView2.setCardBackgroundColor(Color.parseColor("#AAFF00")) // Green color
                cardView2.isClickable = true
                cardView2.isFocusable = true
                productCard.setCardBackgroundColor(Color.WHITE) // Set the main CardView to white

            } else if (material.status == "Non-available") {
                cardView2.setCardBackgroundColor(Color.parseColor("#FF0000")) // Red color
                cardView2.isClickable = false
                cardView2.isFocusable = false
                productCard.setCardBackgroundColor(Color.parseColor("#D3D3D3")) // Set the main CardView to bright grey
            }
            if (material.imageUrl.isNotEmpty()) {
                val pathReference = storage.getReferenceFromUrl(material.imageUrl)
                pathReference.downloadUrl.addOnSuccessListener { uri ->
                    Log.d("MaterialAdapter", "Successfully fetched URI: $uri")
                    Glide.with(holder.itemView).load(uri).into(imageView)
                }.addOnFailureListener { exception ->
                    Log.e("MaterialAdapter", "Failed to load image", exception)
                    imageView.setImageResource(R.drawable.default_book_logo)
                }
            } else {
                imageView.setImageResource(R.drawable.default_book_logo)
                Log.e("MaterialAdapter", "Failed to load image because of empty")
            }

        }

        // Set an onClick listener for the item
        if (material.status == "Available") {
            holder.itemView.setDebouncedOnClickListener {
                // Increment view count
                incrementViewCount(material.id)

                Log.d("MaterialAdapter", "Navigating with Material ID: ${material.id}")
                val action = HomeFragmentDirections.actionHomeFragmentToMaterialDetailsFragment(material)
                it.findNavController().navigate(action)
            }
        }


    }


    private fun incrementViewCount(materialId: String) {
        val firestore = FirebaseFirestore.getInstance()
        val materialRef = firestore.collection("Materials").document(materialId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(materialRef)
            val newViewValue = snapshot.getLong("view")?.plus(1) ?: 1L
            transaction.update(materialRef, "view", newViewValue)
        }.addOnSuccessListener {
            Log.d("MaterialAdapter", "Successfully incremented view count.")
        }.addOnFailureListener { exception ->
            Log.w("MaterialAdapter", "Error incrementing view count.", exception)
        }
    }

    override fun getItemCount(): Int {
        val count = differ.currentList.size
        Log.d("MaterialAdapter", "Item count: $count")
        return count
    }
}
