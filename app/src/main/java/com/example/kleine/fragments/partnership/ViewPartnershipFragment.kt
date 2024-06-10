package com.example.kleine.fragments.partnership

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.kleine.R
import com.example.kleine.databinding.FragmentViewPartnershipBinding
import com.example.kleine.model.AppDatabase
import com.example.kleine.model.Partnership
import com.example.kleine.model.PartnershipDao
import com.example.kleine.model.PartnershipEntity
import com.example.kleine.model.PartnershipStatus
import com.example.kleine.model.Status
import com.example.kleine.resource.NetworkReceiver
import com.example.kleine.viewmodel.partnership.PartnershipViewModel
import com.example.kleine.viewmodel.user.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

interface OnViewPartnerPdfClickListener {
    fun onPdfClick(pdfUrl: String, documentName: String, isFirstPdf: Boolean, partnershipId: String)
}
class ViewPartnershipFragment : Fragment(), OnViewPartnerPdfClickListener {
    val TAG = "ViewPartnershipFragment"
    private lateinit var binding: FragmentViewPartnershipBinding
    private val partnershipViewModel: PartnershipViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private var partnershipId: String? = null
    private lateinit var database: AppDatabase
    private lateinit var partnershipDao: PartnershipDao
    private var isNetworkAvailable: Boolean = false
    private val networkReceiver = NetworkReceiver(
        onNetworkAvailable = {
            isNetworkAvailable = true
        },
        onNetworkUnavailable = {
            isNetworkAvailable = false
        }
    )

    override fun onCreateView (
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        database = AppDatabase.getDatabase(requireContext())
        partnershipDao = database.partnershipDao()
        binding = FragmentViewPartnershipBinding.inflate(inflater, container, false)

        binding.closePdfButton.setOnClickListener {
            binding.pdfView.visibility = View.GONE
            it.visibility = View.GONE  // Hide the close button
        }
        binding.quitPartnership.setOnClickListener {
            if (isNetworkAvailable) {
                quitPartnership()
            }else {
                showNoInternetDialog()
            }
        }


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchPartnerDetailsFromFirestore()
        onViewMaterialClick()
        onUpdatePartnershipClick()

    }

    private fun onViewMaterialClick() {
        binding.viewMaterial.setOnClickListener {
            findNavController().navigate(R.id.action_viewPartnershipFragment_to_partnershipViewMaterialFragment)
        }
    }

    private fun quitPartnership() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("Partnerships")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val documentRef = querySnapshot.documents[0].reference
                        documentRef.update("status", PartnershipStatus.quit)
                            .addOnSuccessListener {
                                val partnershipRef = db.collection("users").document(userId)
                                partnershipRef.update("status", Status.USERS)
                                Log.d(TAG, "Partnership status successfully updated to 'quit'")
                                // Navigating back to the previous fragment
                                findNavController().popBackStack()
                            }
                            .addOnFailureListener { exception ->
                                Log.d(TAG, "Error updating partnership status: ", exception)
                            }
                    } else {
                        Log.d(TAG, "No such partnership exists for this user")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "Error finding partnership: ", exception)
                }
        }
    }


    private fun onUpdatePartnershipClick() {
        binding.updatePartnership.setOnClickListener {
            findNavController().navigate(R.id.action_viewPartnershipFragment_to_updatePartnershipFragment)
        }
    }
    private fun displayPartnerDetails(partner: Partnership) {

        binding.institutionNameText.text = partner.instiName
        binding.institutionTypeText.text = partner.instiType
        binding.locationText.text = partner.location
        binding.contactNumText.text = partner.contactNum

        val pdfFilesName = partner.documentationName.split("|")
        val pdfFiles = partner.documentation.split("|")
        if (pdfFilesName.isNotEmpty()) {
            binding.documentText1.text = pdfFilesName[0]
            binding.documentText1.setOnClickListener {
                onPdfClick(pdfFiles[0], pdfFilesName[0], true,partner.id)
            }
        }

        if (pdfFilesName.size >= 2) {
            binding.documentText2.text = pdfFilesName[1]
            binding.documentText2.setOnClickListener {
                onPdfClick(pdfFiles[1], pdfFilesName[1], false,partner.id)
            }
        } else {
            binding.documentText2.visibility = View.GONE
        }

        userViewModel.fetchUserName(partner.userId) { userName ->
            if (userName != null) {
                binding.username.text = userName
            }
        }
        userViewModel.fetchUserEmail(partner.userId) { email ->
            if (email != null) {
                binding.email.text = email
            }
        }
        userViewModel.fetchUserImage(partner.userId) { userImage ->
            if (userImage != null) {
                val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(userImage)
                storageReference.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(binding.root.context)
                        .load(uri.toString())
                        .into(binding.imageView)
                }
            }
        }
    }
    override fun onPdfClick(pdfUrl: String, documentName: String, isFirstPdf: Boolean, partnershipId: String) {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Download PDF")
            .setMessage("Do you want to download the PDF for offline viewing?")
            .setPositiveButton("Download") { dialog, which ->
                if (isNetworkAvailable) {
                    downloadAndSavePdf(pdfUrl, documentName, isFirstPdf)
                } else {
                    showNoInternetDialog()
                }

            }
            .setNeutralButton("View Online") { dialog, which ->
                if (isNetworkAvailable) {
                    partnershipViewModel.loadPdfIntoView(
                        pdfUrl,
                        binding.pdfView,
                        binding.closePdfButton
                    )
                } else {
                    showNoInternetDialog()
                }

            }
            .setNegativeButton("View Offline") { dialog, which ->
                val pdfPaths = runBlocking {
                    database.partnershipDao().getDocumentationLocalPath(partnershipId)?.split("|")
                }
                if (pdfPaths != null && pdfPaths.isNotEmpty()) {

                    if(isFirstPdf){
                        val firstPdfPath = pdfPaths[0]
                        val pdfFile = File(firstPdfPath)
                        if (pdfFile.exists()) {
                            openPdfFile(firstPdfPath)
                        } else {
                            // PDF file not found, show a message
                            Toast.makeText(requireContext(), "PDF not found on device.", Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        val secondPdfPath = pdfPaths[1]
                        val pdfFile = File(secondPdfPath)
                        if (pdfFile.exists()) {
                            openPdfFile(secondPdfPath)
                        } else {
                            // PDF file not found, show a message
                            Toast.makeText(requireContext(), "PDF not found on device.", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    // PDF path not found in the database, show a message
                    Toast.makeText(requireContext(), "PDF path not found.", Toast.LENGTH_SHORT).show()
                }
            }

            .create()

        alertDialog.show()
    }
    private fun fetchPartnerDetailsFromFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("Partnerships")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val documentSnapshot = querySnapshot.documents[0]
                        val partnership = documentSnapshot.toObject(Partnership::class.java)
                        if (partnership != null) {
                            // Retrieve the document ID from the document snapshot
                            val documentId = documentSnapshot.id
                            partnershipId = documentId
                            Log.d(TAG, "Document ID: $documentId")
                            partnership.id = documentId
                            val partnershipEntity = partnership.toEntity()
                            insertPartnershipIntoRoomDB(partnershipEntity)
                            displayPartnerDetails(partnership)
                        } else {
                            Log.d(TAG, "No partner found")
                        }
                    } else {
                        Log.d(TAG, "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "Error getting documents: ", exception)
                }
        }
    }
    private fun updateRoomDatabaseWithFilePath(partnershipId: String, filePath: String) {
        lifecycleScope.launch {
            database.partnershipDao().updateDocumentationLocalPath(partnershipId, filePath)
        }
    }
    private fun insertPartnershipIntoRoomDB(partnershipEntity: PartnershipEntity) {
        lifecycleScope.launch {
            val existingEntity = database.partnershipDao().getPartnershipById(partnershipEntity.id)
            if (existingEntity != null) {
                database.partnershipDao().update(
                    id = partnershipEntity.id,
                    instiName = partnershipEntity.instiName,
                    instiType = partnershipEntity.instiType,
                    location = partnershipEntity.location,
                    contactNum = partnershipEntity.contactNum,
                    reason = partnershipEntity.reason,
                    documentation = partnershipEntity.documentation,
                    documentationName = partnershipEntity.documentationName,
                    userId = partnershipEntity.userId,
                    status = partnershipEntity.status
                )
            } else {
                database.partnershipDao().insert(partnershipEntity)
            }
        }
    }
    private fun openPdfFile(pdfPath: String) {
        val pdfFile = File(pdfPath)
        if (pdfFile.exists()) {
            binding.pdfView.fromFile(pdfFile)
            binding.pdfView.fromFile(pdfFile)
                .load()

            binding.pdfView.bringToFront()
            binding.pdfView.visibility = View.VISIBLE
            binding.closePdfButton.visibility = View.VISIBLE
        } else {
            Toast.makeText(requireContext(), "PDF file not found.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireActivity().registerReceiver(networkReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(networkReceiver)
    }
    private fun showNoInternetDialog() {
        // Inflate the layout for the dialog
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.no_internet_dialog, null)

        // Create the AlertDialog
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Set up the click listener for the "OK" button in the dialog
        val btnOk = dialogView.findViewById<Button>(R.id.btn_ok)
        btnOk.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }
    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID == id) {
                // Download is complete, update the Room Database with the updated downloadedPdfLocations string
                updateRoomDatabaseWithFilePath(partnershipId.toString(), downloadedPdfLocations)
            }
        }
    }
    private var downloadID: Long = 0L
    private lateinit var file: File
    private var downloadedPdfLocations = "|"
    private fun downloadAndSavePdf(pdfUrl: String, documentName: String, isFirstPdf: Boolean) {
        val downloadManager = requireActivity().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val uri = Uri.parse(pdfUrl)
        val request = DownloadManager.Request(uri)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        // Set the local destination for the downloaded file to a path within the application's external files directory
        file = File(requireActivity().getExternalFilesDir(null), documentName)
        request.setDestinationUri(Uri.fromFile(file))

        // Enqueue a new download and get the reference ID
        downloadID = downloadManager.enqueue(request)

        // Register a BroadcastReceiver to listen for the completion of the download
        requireActivity().registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        // Update the downloadedPdfLocations string based on whether it is the first or second PDF
        val currentPaths = downloadedPdfLocations.split("|").toMutableList()
        if (isFirstPdf) {
            currentPaths[0] = file.absolutePath
        } else {
            currentPaths[1] = file.absolutePath
        }
        downloadedPdfLocations = currentPaths.joinToString("|")
    }

}