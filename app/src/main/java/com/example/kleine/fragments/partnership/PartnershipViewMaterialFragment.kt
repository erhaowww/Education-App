package com.example.kleine.fragments.partnership

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kleine.R
import com.example.kleine.databinding.FragmentPartnershipViewMaterialBinding
import com.example.kleine.databinding.RecyclerViewMaterialDataBinding
import com.example.kleine.model.MaterialData
import com.example.kleine.viewmodel.material.MaterialViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class PartnershipViewMaterialFragment : Fragment() {

    val TAG = "PartnershipViewMaterialFragment"
    private lateinit var binding: FragmentPartnershipViewMaterialBinding
    private val materialViewModel: MaterialViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPartnershipViewMaterialBinding.inflate(inflater, container, false)

        val materialAdapter = MaterialAdapter(listOf())
        binding.materialData.adapter = materialAdapter
        // Observe the material list LiveData from the ViewModel
        materialViewModel.materialList.observe(viewLifecycleOwner, Observer { materials ->
            // Update the adapter's materialList when the LiveData changes
            materialAdapter.materialList = materials
            materialAdapter.notifyDataSetChanged()
        })
        materialViewModel.fetchMaterialsData()

        return binding.root
    }

    inner class MaterialViewHolder(private val itemBinding: RecyclerViewMaterialDataBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(material: MaterialData) {
            itemBinding.materialName.text = material.name
            itemBinding.materialDesc.text = material.desc
            itemBinding.materialRequirement.text = "Category: ${material.category}"
            itemBinding.ratingBar.rating = material.rating.toFloat()

            if (material.imageUrl.isNotEmpty()) {
                val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(material.imageUrl)
                storageReference.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(itemBinding.root.context)
                        .load(uri.toString())
                        .into(itemBinding.image)
                }
            }



            onViewMaterialClick(itemBinding, material.id)
            onViewQuizClick(itemBinding, material.id)
            setupPopupMenu(itemBinding.threeDotsImage, material.id,material.status)
        }
    }

    inner class MaterialAdapter(var materialList: List<MaterialData>) : RecyclerView.Adapter<MaterialViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val itemBinding = RecyclerViewMaterialDataBinding.inflate(inflater, parent, false)
            return MaterialViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {
            holder.bind(materialList[position])
        }

        override fun getItemCount(): Int {
            return materialList.size
        }
    }

    private fun onViewMaterialClick(itemBinding: RecyclerViewMaterialDataBinding, id: String) {
        itemBinding.materialViewData.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("documentId", id)
            findNavController().navigate(R.id.action_partnershipViewMaterialFragment_to_partnershipViewMaterialDetailFragment, bundle)
        }
    }

    private fun onViewQuizClick(itemBinding: RecyclerViewMaterialDataBinding, id: String) {
        itemBinding.materialViewQuiz.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("materialDocId", id)
            findNavController().navigate(R.id.action_partnershipViewMaterialFragment_to_setsFragment, bundle)
        }
    }

    private fun setupPopupMenu(threeDotsImageView: ImageView, materialId: String, status: String) {
        threeDotsImageView.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.menuInflater.inflate(R.menu.popup, popupMenu.menu)

            // Based on the status, decide which menu items to show or hide
            if (status == "Available") {
                popupMenu.menu.findItem(R.id.enable_material).isVisible = false
            } else if (status == "Non-available") {
                popupMenu.menu.findItem(R.id.disable_material).isVisible = false
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.edit_material -> {
                        // Create a bundle with the materialId
                        val bundle = Bundle()
                        bundle.putString("materialId", materialId)

                        // Navigate to EditMaterialFragment with the bundle as arguments
                        findNavController().navigate(R.id.action_partnershipViewMaterialFragment_to_editMaterialFragment, bundle)
                        true
                    }                    R.id.disable_material -> {
                        showConfirmationDialog(materialId, "Disable Material", "Non-available", "Material disabled successfully")
                        true
                    }
                    R.id.enable_material -> {
                        showConfirmationDialog(materialId, "Enable Material", "Available", "Material enabled successfully")
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }



    private fun showConfirmationDialog(materialId: String, title: String, newStatus: String, successMessage: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage("Are you sure you want to $title this material?")
            .setPositiveButton(android.R.string.yes) { _, _ ->
                // Get a reference to the Firestore collection
                val db = FirebaseFirestore.getInstance()
                val materialRef = db.collection("Materials").document(materialId)

                // Update the status
                materialRef.update("status", newStatus)
                    .addOnSuccessListener {
                        // Handle success
                        Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        // Handle failure
                        Toast.makeText(context, "Error updating material: $e", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton(android.R.string.no, null)
            .show()
    }
}


