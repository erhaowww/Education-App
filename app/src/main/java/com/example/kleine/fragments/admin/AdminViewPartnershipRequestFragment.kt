package com.example.kleine.fragments.admin

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kleine.R
import com.example.kleine.databinding.FragmentAdminViewPartnershipRequestBinding
import com.example.kleine.databinding.RecyclerViewAdminViewPartnershipRequestBinding
import com.example.kleine.model.AppDatabase
import com.example.kleine.model.Partnership
import com.example.kleine.model.PartnershipEntity
import com.example.kleine.model.PartnershipStatus
import com.example.kleine.model.Status
import com.example.kleine.viewmodel.partnership.PartnershipViewModel
import com.example.kleine.viewmodel.user.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import com.example.kleine.model.PartnershipDao
import com.example.kleine.resource.NetworkReceiver
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File


interface OnRequestPdfClickListener {
    fun onPdfClick(pdfUrl: String, documentName: String, isFirstPdf: Boolean, partnershipId: String)
}
class AdminViewPartnershipRequestFragment : Fragment(), OnRequestPdfClickListener {
    val TAG = "AdminViewPartnershipRequestFragment"
    private lateinit var binding: FragmentAdminViewPartnershipRequestBinding
    private val partnershipViewModel: PartnershipViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private var partnershipAdapter = PartnershipAdapter(listOf(), this)
    private var isNetworkAvailable: Boolean = false
    private var partnershipId: String? = null
    private lateinit var database: AppDatabase
    private lateinit var partnershipDao: PartnershipDao
    private val networkReceiver = NetworkReceiver(
        onNetworkAvailable = {
            isNetworkAvailable = true
            partnershipAdapter.setNetworkAvailability(true)
        },
        onNetworkUnavailable = {
            isNetworkAvailable = false
            partnershipAdapter.setNetworkAvailability(false)
        }
    )
    override fun onCreateView (
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        database = AppDatabase.getDatabase(requireContext())
        partnershipDao = database.partnershipDao()

        binding = FragmentAdminViewPartnershipRequestBinding.inflate(inflater, container, false)

        binding.partnershipRequestData.adapter = partnershipAdapter



        partnershipViewModel.fetchPendingPartnerships()

        binding.closePdfButton.setOnClickListener {
            binding.pdfView.visibility = View.GONE
            it.visibility = View.GONE  // Hide the close button
        }
        partnershipViewModel.partnershipsList.observe(viewLifecycleOwner, Observer { partnerships ->
            partnershipAdapter.partnershipsList = partnerships
            partnershipAdapter.notifyDataSetChanged()
        })
        return binding.root
    }
    inner class PartnershipAdapter(var partnershipsList: List<Partnership>, private val pdfClickListener: OnRequestPdfClickListener) : RecyclerView.Adapter<PartnershipViewHolder>() {
        var isNetworkAvailable: Boolean = false

        fun setNetworkAvailability(isAvailable: Boolean) {
            isNetworkAvailable = isAvailable
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewPartnershipRequestFragment.PartnershipViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val itemBinding = RecyclerViewAdminViewPartnershipRequestBinding.inflate(inflater, parent, false)
            return PartnershipViewHolder(itemBinding, pdfClickListener)
        }


        override fun onBindViewHolder(holder: PartnershipViewHolder, position: Int) {
            holder.bind(partnershipsList[position])
        }

        override fun getItemCount(): Int {
            return partnershipsList.size
        }

    }
    inner class PartnershipViewHolder(private val itemBinding: RecyclerViewAdminViewPartnershipRequestBinding, private val pdfClickListener: OnRequestPdfClickListener) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(partnership: Partnership) {

            val partnershipEntity = partnership.toEntity()
            insertPartnershipIntoRoomDB(partnershipEntity)

            partnershipId = partnership.id
            itemBinding.instiNameType.text = partnership.instiName + "\n" + partnership.instiType
            itemBinding.location.text = partnership.location
            itemBinding.contactNum.text = partnership.contactNum
            itemBinding.reason.text = partnership.reason
            val documentation = partnership.documentation
            val documentationName = partnership.documentationName
            val pdfFilesName = documentationName.split("|")
            val pdfFiles = documentation.split("|")
            if (pdfFilesName.isNotEmpty()) {
                itemBinding.pdfFile1.text = pdfFilesName[0]
                itemBinding.pdfFile1.setOnClickListener {
                    pdfClickListener.onPdfClick(pdfFiles[0], pdfFilesName[0], true,partnership.id)
                }
            }

            if (pdfFilesName.size >= 2) {
                itemBinding.pdfFile2.text = pdfFilesName[1]
                itemBinding.pdfFile2.setOnClickListener {
                    pdfClickListener.onPdfClick(pdfFiles[1], pdfFilesName[1], false,partnership.id)
                }
            } else {
                itemBinding.pdfFile2.visibility = View.GONE
            }

            userViewModel.fetchUserName(partnership.userId) { userName ->
                if (userName != null) {
                    itemBinding.nameText.text = userName
                }
            }

            userViewModel.fetchUserImage(partnership.userId) { userImage ->
                if (userImage != null) {
                    val storageReference =
                        FirebaseStorage.getInstance().getReferenceFromUrl(userImage)
                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        Glide.with(itemBinding.root.context)
                            .load(uri.toString())
                            .into(itemBinding.userImg)
                    }
                }
            }

            itemBinding.btnApprove.setOnClickListener {
                if (isNetworkAvailable) {
                    // Show approval dialog
                    showAlertDialog(
                        title = "Approve Partnership",
                        message = "Are you sure you want to approve this user as partner?",
                        partnership = partnership
                    )
                } else {
                    showNoInternetDialog()
                }
            }

            itemBinding.btnReject.setOnClickListener {
                if (isNetworkAvailable) {
                    // Show reject dialog
                    showRejectAlertDialog(
                        title = "Reject Partnership",
                        message = "Are you sure you want to reject this user?",
                        partnership = partnership
                    )
                } else {
                    showNoInternetDialog()
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
    private fun showAlertDialog(title: String, message: String, partnership: Partnership) {
        // Inflate the layout for the dialog
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.delete_alert_dialog, null)

        // Set the title and message
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_delete_item)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_delete_message)
        tvTitle.text = title
        tvMessage.text = message
        // Create the AlertDialog
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        // Set up the click listeners for the buttons in the dialog
        val btnNo = dialogView.findViewById<Button>(R.id.btn_no)
        btnNo.setOnClickListener {
            alertDialog.dismiss()
        }

        val btnYes = dialogView.findViewById<Button>(R.id.btn_yes)
        btnYes.setOnClickListener {
            approvePartnerData(partnership.id, partnership.userId)
            partnershipAdapter.notifyDataSetChanged()
            alertDialog.dismiss()
        }



        alertDialog.show()
    }

    private fun showRejectAlertDialog(title: String, message: String, partnership: Partnership) {
        // Inflate the layout for the dialog
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.reject_partner_alert_dialog, null)  // Assuming the layout's name is 'delete_alert_dialog.xml'

        // Set the title and message
        dialogView.findViewById<TextView>(R.id.tv_delete_item).text = title
        dialogView.findViewById<TextView>(R.id.tv_delete_message).text = message
        // Create the AlertDialog
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        // Set up the click listeners for the buttons in the dialog
        val btnNo = dialogView.findViewById<Button>(R.id.btn_no)
        btnNo.setOnClickListener {
            alertDialog.dismiss()
        }

        val btnYes = dialogView.findViewById<Button>(R.id.btn_yes)
        btnYes.setOnClickListener {
            val reasonMessage = dialogView.findViewById<EditText>(R.id.reason_message).text.toString()
            rejectPartnerData(partnership.id, partnership.userId, reasonMessage)
            partnershipAdapter.notifyDataSetChanged()
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun approvePartnerData(partnershipId: String, userId: String) {
        if (isNetworkAvailable) {
            Toast.makeText(context, "Network is not available. Cannot approve partner.", Toast.LENGTH_SHORT).show()
            return
        }
        val firestore = FirebaseFirestore.getInstance()
        val partnershipRef = firestore.collection("Partnerships").document(partnershipId)

        partnershipRef.update("status", PartnershipStatus.approved) // Assuming the field name is "status"
            .addOnSuccessListener {
                // Now, update the user's status
                val userRef = firestore.collection("users").document(userId)
                userRef.update("status", Status.PARTNERS)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Partner approved successfully!", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun rejectPartnerData(partnershipId: String, userId: String, reasonMsg: String) {
        if (isNetworkAvailable) {
            Toast.makeText(context, "Network is not available. Cannot reject partner.", Toast.LENGTH_SHORT).show()
            return
        }
        val firestore = FirebaseFirestore.getInstance()
        val partnershipRef = firestore.collection("Partnerships").document(partnershipId)

        partnershipRef.update("status", PartnershipStatus.rejected)
        partnershipRef.update("rejectReason", reasonMsg)
            .addOnSuccessListener {
                Toast.makeText(context, "Partner rejected successfully!", Toast.LENGTH_SHORT).show()
            }
    }



    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireContext().registerReceiver(networkReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(networkReceiver)
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
                .load()

            binding.pdfView.bringToFront()
            binding.pdfView.visibility = View.VISIBLE
            binding.closePdfButton.visibility = View.VISIBLE
        } else {
            Toast.makeText(requireContext(), "PDF file not found.", Toast.LENGTH_SHORT).show()
        }
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



}