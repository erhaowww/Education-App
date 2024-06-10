package com.example.kleine.fragments.partnership

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.kleine.R
import com.example.kleine.activities.ShoppingActivity
import com.example.kleine.databinding.FragmentJoinPartnerBinding
import com.example.kleine.databinding.RecyclerViewAdminViewPartnershipBinding
import com.example.kleine.fragments.admin.AdminViewPartnershipFragment
import com.example.kleine.fragments.admin.OnPdfClickListener
import com.example.kleine.model.Partnership
import com.example.kleine.model.PartnershipStatus
import com.example.kleine.model.Status
import com.example.kleine.resource.NetworkReceiver
import com.example.kleine.viewmodel.shopping.ShoppingViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


class JoinPartnerFragment : Fragment() {
    private val TAG = "JoinPartnerFragment"
    private lateinit var binding: FragmentJoinPartnerBinding
    private lateinit var viewModel: ShoppingViewModel
    private lateinit var documentUploadBtn: Button
    private val REQUEST_CODE = 100
    private lateinit var getContent: ActivityResultLauncher<String>
    private val selectedPDFs = ArrayList<Uri>()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val uploadedPDFLinks = ArrayList<String>()
    private var uploadedPDFNames = mutableListOf<String>()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
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
        binding = FragmentJoinPartnerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = (activity as ShoppingActivity).viewModel
        documentUploadBtn = view.findViewById(R.id.documentUploadBtn)
        checkExistingRequest()
        checkAndRequestPermissions()

        getContent = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                selectedPDFs.clear()
                for (uri in uris) {
                    val path = uri.path
                    if (path != null && path.endsWith(".pdf")) {
                        selectedPDFs.add(uri)
                    } else {
                        Toast.makeText(requireContext(), "Only PDF files are allowed", Toast.LENGTH_SHORT).show()
                    }
                }

                if (selectedPDFs.size > 2) {
                    Toast.makeText(requireContext(), "You can only upload up to two PDF files", Toast.LENGTH_SHORT).show()
                    selectedPDFs.clear()
                } else {
                    documentUploadBtn.text = "${selectedPDFs.size} PDF(s) Selected"
                }
            }
        }

        documentUploadBtn.setOnClickListener {
            getContent.launch("application/pdf")
        }

        binding.btnJoin.setOnClickListener{
            if (userId != null) {
                val db = FirebaseFirestore.getInstance()
                db.collection("Partnerships")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val documentRef = querySnapshot.documents[0].reference
                            documentRef.update("status", PartnershipStatus.approved)
                                .addOnSuccessListener {
                                    val partnershipRef = db.collection("users").document(userId)
                                    partnershipRef.update("status", Status.PARTNERS)
                                    Log.d(TAG, "Partnership status successfully updated to approved")
                                    Toast.makeText(requireContext(), "You had join to partnership", Toast.LENGTH_SHORT).show()
                                    findNavController().popBackStack()
                                }
                        }
                    }
            }
        }


        binding.btnRequest.setOnClickListener {
            if (isNetworkAvailable) {
                it.isEnabled = false
                (it as Button).text = "Wait for a while"
                it.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                binding.instiName.isEnabled = false
                binding.instiType.isEnabled = false
                binding.location.isEnabled = false
                binding.contactNo.isEnabled = false
                binding.reason.isEnabled = false
                binding.documentUploadBtn.isEnabled = false
                val errors = StringBuilder()
                // Validation starts here
                if (selectedPDFs.size == 0 || selectedPDFs.size > 2) {
                    errors.append("• Please select up to two PDF files.\n")
                }

                val name = binding.instiName.text.toString()
                if (name.isEmpty()) {
                    errors.append("• Institution name is empty.\n")
                }

                val type = binding.instiType.text.toString()
                if (type.isEmpty()) {
                    errors.append("• Institution type is empty.\n")
                }

                val loc = binding.location.text.toString()
                if (loc.isEmpty()) {
                    errors.append("• Location is empty.\n")
                }

                val contact = binding.contactNo.text.toString()
                val contactPattern = "^0\\d{2}-\\d{7,8}$"
                if (contact.isEmpty()) {
                    errors.append("• Contact number is empty.\n")
                } else if (!contact.matches(contactPattern.toRegex())) {
                    errors.append("• Contact number should be in the format 012-1231231 or 012-12341234.\n")
                }

                val res = binding.reason.text.toString()
                if (res.isEmpty()) {
                    errors.append("• Reason is empty.\n")
                }

                if (errors.isNotEmpty()) {
                    showErrorDialog("Validation Error", errors.toString())
                    it.isEnabled = true
                    it.text = "Request"
                    it.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

                    binding.instiName.isEnabled = true
                    binding.instiType.isEnabled = true
                    binding.location.isEnabled = true
                    binding.contactNo.isEnabled = true
                    binding.reason.isEnabled = true
                    binding.documentUploadBtn.isEnabled = true
                    return@setOnClickListener
                }
                uploadDataToFirestoreAndStorage()
            } else {
                showNoInternetDialog()
            }
        }
    }
    private fun checkExistingRequest() {
        if (userId != "") {
            val query = db.collection("Partnerships")
                .whereEqualTo("userId", userId)

            query.get().addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val status = document.getString("status")
                        val rejectReason = document.getString("rejectReason")
                        if (status == "pending") {
                            binding.requestedText.visibility = View.VISIBLE
                            binding.requestedText.text = "Waiting for response"
                            binding.instiName.visibility = View.GONE
                            binding.instiType.visibility = View.GONE
                            binding.location.visibility = View.GONE
                            binding.contactNo.visibility = View.GONE
                            binding.reason.visibility = View.GONE
                            binding.documentUploadBtn.visibility = View.GONE
                            binding.instiNameText.visibility = View.GONE
                            binding.instiTypeText.visibility = View.GONE
                            binding.locText.visibility = View.GONE
                            binding.contactNoText.visibility = View.GONE
                            binding.reasonText.visibility = View.GONE
                            binding.docText.visibility = View.GONE
                            binding.btnRequest.visibility = View.GONE
                        } else if (status == "rejected") {
                            binding.rejectText.visibility = View.VISIBLE
                            binding.rejectText.text = "Reject Reason: $rejectReason"
                            val instiName = document.getString("instiName") ?: ""
                            val instiType = document.getString("instiType") ?: ""
                            val location = document.getString("location") ?: ""
                            val contactNum = document.getString("contactNum") ?: ""
                            val reason = document.getString("reason") ?: ""
                            // Set the data back into your UI elements
                            binding.instiName.setText(instiName)
                            binding.instiType.setText(instiType)
                            binding.location.setText(location)
                            binding.contactNo.setText(contactNum)
                            binding.reason.setText(reason)
                        } else if (status == "quit") {
                            binding.requestedText.visibility = View.VISIBLE
                            binding.requestedText.text = "You Quit The Partners"
                            binding.instiName.visibility = View.GONE
                            binding.instiType.visibility = View.GONE
                            binding.location.visibility = View.GONE
                            binding.contactNo.visibility = View.GONE
                            binding.reason.visibility = View.GONE
                            binding.documentUploadBtn.visibility = View.GONE
                            binding.instiNameText.visibility = View.GONE
                            binding.instiTypeText.visibility = View.GONE
                            binding.locText.visibility = View.GONE
                            binding.contactNoText.visibility = View.GONE
                            binding.reasonText.visibility = View.GONE
                            binding.docText.visibility = View.GONE
                            binding.btnRequest.visibility = View.GONE
                            binding.btnJoin.visibility = View.VISIBLE
                        }
                    }
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error checking for existing request: ", e)
            }
        }
    }
    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE
            )
        }
    }

    private fun uploadDataToFirestoreAndStorage() {
        uploadedPDFLinks.clear()
        uploadedPDFNames.clear()

        db.collection("Partnerships")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "rejected")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val name = binding.instiName.text.toString()
                val type = binding.instiType.text.toString()
                val loc = binding.location.text.toString()
                val contact = binding.contactNo.text.toString()
                val res = binding.reason.text.toString()

                // Prepare data for Firestore
                val data = HashMap<String, Any>()
                data["userId"] = userId
                data["instiName"] = name
                data["instiType"] = type
                data["location"] = loc
                data["contactNum"] = contact
                data["reason"] = res
                data["status"] = "pending"
                data["rejectReason"] = ""

                var uploadedCount = 0
                for (uri in selectedPDFs) {
                    val randomFileName = "${System.currentTimeMillis()}-${uri.lastPathSegment?.split("/")?.last()}"
                    val storageReference: StorageReference = storage.reference.child("partnershipPDF/$randomFileName")
                    storageReference.putFile(uri)
                        .addOnSuccessListener { _ ->
                            storageReference.downloadUrl.addOnSuccessListener { downloadUri ->
                                uploadedPDFLinks.add(downloadUri.toString())
                                uploadedPDFNames.add("${uri.lastPathSegment?.split("/")?.last()}")
                                uploadedCount++

                                if (uploadedCount == selectedPDFs.size) {
                                    data["documentation"] = uploadedPDFLinks.joinToString("|")
                                    data["documentationName"] = uploadedPDFNames.joinToString("|")
                                    if (querySnapshot.isEmpty) {
                                        // No existing rejected request, create a new document
                                        db.collection("Partnerships").add(data)
                                            .addOnSuccessListener {
                                                Toast.makeText(requireContext(), "Partner Request Successfully", Toast.LENGTH_SHORT).show()
                                                findNavController().popBackStack()
                                            }
                                            .addOnFailureListener { exception: Exception ->
                                                Toast.makeText(requireContext(), "Failed to upload data", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        // Existing rejected request, update it
                                        val docId = querySnapshot.documents[0].id
                                        db.collection("Partnerships").document(docId).set(data)
                                            .addOnSuccessListener {
                                                Toast.makeText(requireContext(), "Partner Request Successfully", Toast.LENGTH_SHORT).show()
                                                findNavController().popBackStack()
                                            }
                                            .addOnFailureListener { exception: Exception ->
                                                Toast.makeText(requireContext(), "Failed to update data", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { exception: Exception ->
                            Toast.makeText(requireContext(), "Failed to upload PDF", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception: Exception ->
                Toast.makeText(requireContext(), "Failed to check for existing requests", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showErrorDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.error_message_dialog, null)

        with(dialogLayout) {
            findViewById<TextView>(R.id.tv_error_title).text = title
            findViewById<TextView>(R.id.tv_error_message).text = message
        }

        val okButton = dialogLayout.findViewById<Button>(R.id.btn_ok)
        builder.setView(dialogLayout)
        val alertDialog = builder.create()

        okButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
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
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireActivity().registerReceiver(networkReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(networkReceiver)
    }






}