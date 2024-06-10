package com.example.kleine.fragments.partnership

import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.kleine.R
import com.example.kleine.databinding.FragmentUpdatePartnershipBinding
import com.example.kleine.resource.NetworkReceiver
import com.example.kleine.viewmodel.partnership.PartnershipViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class UpdatePartnershipFragment : Fragment() {
    val TAG = "UpdatePartnershipFragment"
    private lateinit var binding: FragmentUpdatePartnershipBinding
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private lateinit var partnershipViewModel: PartnershipViewModel
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
        binding = FragmentUpdatePartnershipBinding.inflate(inflater, container, false)

        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        partnershipViewModel = ViewModelProvider(this).get(PartnershipViewModel::class.java)

        // Observers for LiveData
        partnershipViewModel.isDataUpdated.observe(viewLifecycleOwner) { isUpdated ->
            if (isUpdated) {
                Toast.makeText(
                    requireContext(),
                    "Partnership Updated Successfully",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack()
            }
        }

        loadExistingData()

        binding.reqBtnRequest.setOnClickListener {
            if (isNetworkAvailable) {
                it.isEnabled = false
                (it as Button).text = "Wait for a while"
                it.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                binding.reqInstiName.isEnabled = false
                binding.reqInstiType.isEnabled = false
                binding.reqLocation.isEnabled = false
                binding.reqContactNo.isEnabled = false
                val errors = StringBuilder()

                val name = binding.reqInstiName.text.toString()
                if (name.isEmpty()) {
                    errors.append("• Institution name is empty.\n")
                }

                val type = binding.reqInstiType.text.toString()
                if (type.isEmpty()) {
                    errors.append("• Institution type is empty.\n")
                }

                val loc = binding.reqLocation.text.toString()
                if (loc.isEmpty()) {
                    errors.append("• Location is empty.\n")
                }

                val contact = binding.reqContactNo.text.toString()
                val contactPattern = "^0\\d{2}-\\d{7,8}$"
                if (contact.isEmpty()) {
                    errors.append("• Contact number is empty.\n")
                } else if (!contact.matches(contactPattern.toRegex())) {
                    errors.append("• Contact number should be in the format like 012-1234567 or 012-12345678.\n")
                }

                if (errors.isNotEmpty()) {
                    showErrorDialog("Validation Error", errors.toString())
                    it.isEnabled = true
                    it.text = "Update"
                    it.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

                    binding.reqInstiName.isEnabled = true
                    binding.reqInstiType.isEnabled = true
                    binding.reqLocation.isEnabled = true
                    binding.reqContactNo.isEnabled = true
                    return@setOnClickListener
                }
                partnershipViewModel.updateRequestDataToFirestore(
                    userId,
                    binding.reqInstiName.text.toString(),
                    binding.reqInstiType.text.toString(),
                    binding.reqLocation.text.toString(),
                    binding.reqContactNo.text.toString()
                )
            }else {
                showNoInternetDialog()
            }
        }
    }

    private fun updateRequestDataToFirestore() {
        val name = binding.reqInstiName.text.toString()
        val type = binding.reqInstiType.text.toString()
        val loc = binding.reqLocation.text.toString()
        val contact = binding.reqContactNo.text.toString()

        val errors = StringBuilder()
        // Validation starts here
        if (name.isEmpty()) {
            errors.append("• Institution name is empty.\n")
        }
        if (type.isEmpty()) {
            errors.append("• Institution type is empty.\n")
        }
        if (loc.isEmpty()) {
            errors.append("• Location is empty.\n")
        }
        val contactPattern = "^0\\d{2}-\\d{7,8}$"
        if (contact.isEmpty()) {
            errors.append("• Contact number is empty.\n")
        } else if (!contact.matches(contactPattern.toRegex())) {
            errors.append("• Contact number should be in the format 012-1231231 or 012-12341234.\n")
        }

        if (errors.isNotEmpty()) {
            showErrorDialog("Validation Error", errors.toString())
            return
        }

        // Prepare data for Firestore
        val data = HashMap<String, Any>()
        data["instiName"] = name
        data["instiType"] = type
        data["location"] = loc
        data["contactNum"] = contact

        db.collection("Partnerships")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.documents.isNotEmpty()) {
                    val docId = querySnapshot.documents[0].id
                    db.collection("Partnerships").document(docId).update(data)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Partnership Updated Successfully", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                        .addOnFailureListener { exception: Exception ->
                            Toast.makeText(requireContext(), "Failed to update data", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "No existing partnership found to update!", Toast.LENGTH_SHORT).show()
                }
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

    private fun loadExistingData() {
        db.collection("Partnerships")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.documents.isNotEmpty()) {
                    val document = querySnapshot.documents[0]
                    binding.reqInstiName.setText(document.getString("instiName"))
                    binding.reqInstiType.setText(document.getString("instiType"))
                    binding.reqLocation.setText(document.getString("location"))
                    binding.reqContactNo.setText(document.getString("contactNum"))
                }
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