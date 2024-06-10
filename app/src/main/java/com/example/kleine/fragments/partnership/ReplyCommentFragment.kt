package com.example.kleine.fragments.partnership

import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.kleine.R
import com.example.kleine.databinding.FragmentReplyCommentBinding
import com.example.kleine.resource.NetworkReceiver
import com.example.kleine.viewmodel.comment.CommentViewModel
import com.example.kleine.viewmodel.material.MaterialViewModel
import com.example.kleine.viewmodel.user.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReplyCommentFragment : Fragment() {
    val TAG = "ReplyCommentFragment"
    private lateinit var binding: FragmentReplyCommentBinding
    val commentViewModel: CommentViewModel by viewModels {
        CommentViewModel.CommentViewModelFactory(userViewModel)
    }
    private val db = FirebaseFirestore.getInstance()
    private val userViewModel: UserViewModel by viewModels()
    private val materialViewModel: MaterialViewModel by viewModels()
    private var isNetworkAvailable: Boolean = false
    private val networkReceiver = NetworkReceiver(
        onNetworkAvailable = {
            isNetworkAvailable = true
        },
        onNetworkUnavailable = {
            isNetworkAvailable = false
        }
    )
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReplyCommentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val commentDocumentId = arguments?.getString("commentDocumentId") ?: return
        Log.d(TAG, "Data received: $commentDocumentId")

        // Fetch the comment and update UI
        commentViewModel.fetchCommentToReply(commentDocumentId) { commentWithUserDetails ->
            val comment = commentWithUserDetails.comment

            binding.commentDetailDateName.text = "${comment.commentDate} | ${commentWithUserDetails.userName}"
            binding.rating.text = comment.rating.toString()
            binding.userCommentText.text = comment.comment

            // Fetch the Material data and update UI
            materialViewModel.fetchMaterialForComment(comment.materialId) { name, imageUrl ->
                binding.subjectTitle.text = name
                if (imageUrl == "" || imageUrl == null) {
                    binding.subjectImg.setImageResource(R.drawable.default_book_logo)
                }else{
                    val storageReference =
                        FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                    storageReference.downloadUrl
                        .addOnSuccessListener { uri ->
                            Glide.with(binding.subjectImg.context)
                                .load(uri.toString())
                                .into(binding.subjectImg)
                        }
                        .addOnFailureListener { e ->
                            binding.subjectImg.setImageResource(R.drawable.default_book_logo)

                        }
                }
            }

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@fetchCommentToReply
            userViewModel.fetchUserImage(comment.userId) { userImage ->
                val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(userImage.toString())
                storageReference.downloadUrl
                    .addOnSuccessListener { uri ->
                    Glide.with(binding.postDetailUserImg.context)
                        .load(uri.toString())
                        .into(binding.postDetailUserImg)
                }
            }
            userViewModel.fetchUserImage(currentUserId) { userImage ->
                val storageReference =
                    FirebaseStorage.getInstance().getReferenceFromUrl(userImage.toString())
                storageReference.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(binding.replier.context)
                        .load(uri.toString())
                        .into(binding.replier)
                }
            }
            // Set the onClickListener for the reply button
            binding.postDetailAddCommentBtn.setOnClickListener { btn ->
                if (isNetworkAvailable) {
                    btn.isEnabled = false

                    (btn as Button).setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            android.R.color.darker_gray
                        )
                    )
                    binding.postDetailComment.isEnabled = false

                    val errors = StringBuilder()
                    // Validation starts here
                    val commentText = binding.postDetailComment.text.toString()
                    if (commentText.isBlank()) {
                        errors.append("• Comment cannot be empty.\n")
                    }
                    Toast.makeText(requireContext(), "Posting...", Toast.LENGTH_SHORT).show()
                    if (errors.isNotEmpty()) {
                        showErrorDialog("Validation Error", errors.toString())
                        btn.isEnabled = true
                        btn.text = "Reply"
                        btn.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                android.R.color.white
                            )
                        )
                        binding.postDetailComment.isEnabled = true
                        return@setOnClickListener
                    }

                    // If no errors, proceed to post the comment
                    val currentTime =
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    val currentUserId =
                        FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

                    val replyData = mapOf(
                        "replyPartnerId" to currentUserId,
                        "replyDate" to currentTime,
                        "replyComment" to commentText
                    )

                    db.collection("Comments").document(commentDocumentId)
                        .update(replyData)
                        .addOnSuccessListener {
                            // Handle success, e.g., navigate to another screen or display a success message
                            Toast.makeText(
                                requireContext(),
                                "Reply posted successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            val fragmentManager = requireActivity().supportFragmentManager
                            fragmentManager.popBackStack()
                        }
                        .addOnFailureListener { e ->
                            // Handle error, re-enable EditText and Button and display an error message
                            showErrorDialog("Error", e.message ?: "Failed to post comment.")
                            btn.isEnabled = true
                            btn.text = "Reply"
                            btn.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    android.R.color.white
                                )
                            )
                            binding.postDetailComment.isEnabled = true
                        }
                } else {
                    showNoInternetDialog()
                }
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
}
