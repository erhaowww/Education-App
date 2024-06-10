package com.example.kleine.viewmodel.material

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kleine.model.CourseDocument
import com.example.kleine.model.Material
import com.example.kleine.model.MaterialData
import com.example.kleine.model.MaterialEngageData
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class MaterialViewModel : ViewModel() {
    val materialEngageData = MutableLiveData<MaterialEngageData?>()
    private val _materialList = MutableLiveData<List<MaterialData>>()
    val materialList: LiveData<List<MaterialData>> = _materialList

    private val storageRef = FirebaseStorage.getInstance().reference
    private val db = FirebaseFirestore.getInstance()

    fun fetchMaterialsData() {
        val db = FirebaseFirestore.getInstance()
        db.collection("Materials")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                val tempList = ArrayList<MaterialData>()
                for (document in snapshots!!) {
                    val id = document.id
                    val materialName = document.getString("name") ?: ""
                    val description = document.getString("desc") ?: ""
                    val requirement = document.getString("requirement") ?: ""
                    val rating = document.getDouble("rating") ?: 0.0
                    val imageUrl = document.getString("imageUrl") ?: ""
                    val status = document.getString("status") ?: ""
                    tempList.add(MaterialData(id, materialName, description, requirement, rating, imageUrl, status))
                }
                _materialList.value = tempList
            }
    }
    fun fetchMaterialsEngageData(documentId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Materials").document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val materialOverview = MaterialEngageData(
                        document.getString("name") ?: "",
                        document.getLong("view") ?: 0,
                        document.getLong("enroll") ?: 0,
                        document.getLong("graduate") ?: 0,
                        document.getString("imageUrl") ?: ""
                    )
                    materialEngageData.postValue(materialOverview)
                }
            }

    }

    fun addMaterial(material: Material, imageUri: Uri?, documentUri: Uri?) {
        val materialRef = db.collection("Materials").document()

        material.id = materialRef.id

        val uploadTasks = mutableListOf<Task<*>>()

        imageUri?.let { uri ->
            val imageRef = storageRef.child("images/${material.id}")
            uploadTasks.add(imageRef.putFile(uri).continueWithTask {
                imageRef.downloadUrl
            }.addOnSuccessListener { url ->
                material.imageUrl = url.toString()
            })
        }

        documentUri?.let { uri ->
            val docRef = storageRef.child("documents/${material.id}")
            uploadTasks.add(docRef.putFile(uri).continueWithTask {
                docRef.downloadUrl
            }.addOnSuccessListener { url ->
                // Add the document URL to a Course object and save it to the "Courses" sub-collection
                val course = CourseDocument(documentUrl = url.toString())
                materialRef.collection("Courses").add(course)
                    .addOnSuccessListener {
                        Log.d("MaterialViewModel", "Document successfully added to Courses sub-collection")
                    }
                    .addOnFailureListener { e ->
                        Log.e("MaterialViewModel", "Error adding document to Courses sub-collection", e)
                    }
            })
        }


        Tasks.whenAllSuccess<Any>(uploadTasks).addOnSuccessListener {
            materialRef.set(material)
        }.addOnFailureListener { e ->
            Log.e("MaterialViewModel", "Error adding material", e)
        }
    }




    fun fetchMaterialForComment(documentId: String, onComplete: (name: String, imageUrl: String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Materials").document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val name = document.getString("name") ?: ""
                    val imageUrl = document.getString("imageUrl") ?: ""
                    onComplete(name, imageUrl)
                }
            }
    }



}
