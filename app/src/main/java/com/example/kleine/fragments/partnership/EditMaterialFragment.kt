package com.example.kleine.fragments.partnership

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.kleine.R
import com.example.kleine.databinding.EditMaterialBinding
import com.example.kleine.model.Material
import com.example.kleine.viewmodel.material.MaterialViewModel
import com.google.firebase.firestore.FirebaseFirestore

class EditMaterialFragment : Fragment() {

    private lateinit var binding: EditMaterialBinding
    private val materialViewModel: MaterialViewModel by viewModels()
    private var materialId: String? = null
    private var selectedImageUri: Uri? = null
    private var selectedDocumentUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = EditMaterialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve materialId from arguments
        materialId = arguments?.getString("materialId")

        if (materialId == null) {
            Toast.makeText(context, "Error: Material ID not provided", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch material details and set initial values
        val db = FirebaseFirestore.getInstance()
        val materialRef = db.collection("Materials").document(materialId!!)
        materialRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val material = document.toObject(Material::class.java)
                material?.let {
                    binding.editTextName.setText(it.name)
                    binding.editTextDesc.setText(it.desc)
                    val categoryId = when (it.category) {
                        "Easy" -> R.id.radioButtonEasy
                        "Medium" -> R.id.radioButtonMedium
                        "Advanced" -> R.id.radioButtonAdvanced
                        else -> -1
                    }
                    binding.radioGroupCategory.check(categoryId)
                    binding.textViewMaterialID.text = "Material ID: ${materialId}"
                    val radioButtonId = if (it.status == "Available") R.id.radioButtonAvailable else R.id.radioButtonUnavailable
                    binding.radioGroupStatus.check(radioButtonId)
                }
            }
        }

        // Set onClickListeners for buttons
        binding.buttonSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_CODE_IMAGE_PICK)
        }

        binding.buttonUploadDocument.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*" // Set type to */*
            // Use Intent.EXTRA_MIME_TYPES to allow both images and PDFs
            intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))
            startActivityForResult(intent, REQUEST_CODE_DOCUMENT_PICK)
        }
        binding.buttonUpdate.setOnClickListener { updateMaterial() }
    }

    private fun updateMaterial() {
        val name = binding.editTextName.text.toString()
        val description = binding.editTextDesc.text.toString()
        val selectedCategoryId = binding.radioGroupCategory.checkedRadioButtonId
        val selectedCategoryButton = view?.findViewById<RadioButton>(selectedCategoryId)
        val category = selectedCategoryButton?.text.toString() ?: ""
        val selectedStatusId = binding.radioGroupStatus.checkedRadioButtonId
        val selectedRadioButton = view?.findViewById<RadioButton>(selectedStatusId)
        val status = selectedRadioButton?.text.toString()

        if (name.isBlank() || description.isBlank() || category.isBlank() || status.isBlank()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedMaterial = Material(
            id = materialId!!,
            name = name,
            desc = description,
            category = category,
            status = status
        )

        val db = FirebaseFirestore.getInstance()
        val materialRef = db.collection("Materials").document(materialId!!)
        materialRef.set(updatedMaterial).addOnSuccessListener {
            Toast.makeText(context, "Material updated successfully", Toast.LENGTH_SHORT).show()

            // Obtain NavController and navigate up
            val navController = findNavController()
            navController.navigateUp()

        }.addOnFailureListener {
            Toast.makeText(context, "Error updating material: $it", Toast.LENGTH_SHORT).show()
        }
    }


    // Handle onActivityResult for image and document selection
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_IMAGE_PICK -> {
                    selectedImageUri = data?.data
                }
                REQUEST_CODE_DOCUMENT_PICK -> {
                    selectedDocumentUri = data?.data // Here, remove the val keyword
                    val mimeType = context?.contentResolver?.getType(selectedDocumentUri!!)
                    if (mimeType == "application/pdf" || mimeType?.startsWith("image/") == true) {
                        // Handle the selected PDF or image
                        binding.textViewDocumentStatus.text = "Document has been uploaded."
                    } else {
                        // Show an error message for unsupported file type
                        Toast.makeText(context, "Unsupported file type. Please select an image or PDF.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    companion object {
        private const val REQUEST_CODE_IMAGE_PICK = 1
        private const val REQUEST_CODE_DOCUMENT_PICK = 2
    }
}
