package com.example.kleine.viewmodel.partnership

import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kleine.model.Partnership
import com.example.kleine.model.PartnershipStatus
import com.github.barteksc.pdfviewer.PDFView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage

class PartnershipViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _partnershipsList = MutableLiveData<List<Partnership>>()
    val partnershipsList: LiveData<List<Partnership>> = _partnershipsList
    private var partnershipListenerRegistration: ListenerRegistration? = null
    val isDataUpdated = MutableLiveData<Boolean>()


    fun updateRequestDataToFirestore(userId: String, name: String, type: String, loc: String, contact: String) {
        val data: Map<String, Any> = hashMapOf(
            "instiName" to name,
            "instiType" to type,
            "location" to loc,
            "contactNum" to contact
        )


        db.collection("Partnerships")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.documents.isNotEmpty()) {
                    val docId = querySnapshot.documents[0].id
                    db.collection("Partnerships").document(docId).update(data)
                        .addOnSuccessListener {
                            isDataUpdated.value = true
                        }
                }
            }
    }
    fun fetchApprovedPartnerships() {
        partnershipListenerRegistration?.remove()

        db.collection("Partnerships")
            .whereEqualTo("status", PartnershipStatus.approved.name) // Filter by status
            .get()
            .addOnSuccessListener { result ->
                val partnershipList = mutableListOf<Partnership>()
                for (document in result) {
                    val partnership = document.toObject(Partnership::class.java)
                    partnership.id = document.id
                    partnershipList.add(partnership)
                }

                _partnershipsList.value = partnershipList

            }
    }
    fun fetchPendingPartnerships() {
        partnershipListenerRegistration?.remove()

        partnershipListenerRegistration = db.collection("Partnerships")
            .whereEqualTo("status", PartnershipStatus.pending.name)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val partnershipList = mutableListOf<Partnership>()
                for (document in value!!) {
                    val partnership = document.toObject(Partnership::class.java)
                    partnership.id = document.id
                    partnershipList.add(partnership)
                }
                _partnershipsList.value = partnershipList
            }
    }


    fun loadPdfIntoView(pdfUrl: String, pdfView: PDFView, closePdfButton: View) {
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
        storageReference.getStream().addOnSuccessListener { taskSnapshot ->
            val inputStream = taskSnapshot.stream
            pdfView.bringToFront()  // Brings the PDFView to the front
            pdfView.visibility = View.VISIBLE
            closePdfButton.visibility = View.VISIBLE  // Show the close button
            pdfView.fromStream(inputStream)
                .onLoad { totalPages -> Log.d("PDF_VIEW", "Loaded with total pages: $totalPages") }  // Log when PDF is loaded
                .onError { t -> Log.e("PDF_VIEW", "Error loading PDF", t) }  // Log errors if any
                .load()
        }.addOnFailureListener { e ->
            Log.e("PDF_VIEW", "Error downloading PDF", e)
        }
    }



}