package com.example.kleine.fragments.settings

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kleine.R
import com.example.kleine.SpacingDecorator.VerticalSpacingItemDecorator
import com.example.kleine.activities.ShoppingActivity
import com.example.kleine.adapters.recyclerview.CartRecyclerAdapter
import com.example.kleine.databinding.FragmentOrderDetailsBinding
import com.example.kleine.model.Address
import com.example.kleine.model.CourseDocument
import com.example.kleine.model.Material
import com.example.kleine.model.Order
import com.example.kleine.resource.Resource
import com.example.kleine.util.Constants.Companion.ORDER_CONFIRM_STATE
import com.example.kleine.util.Constants.Companion.ORDER_Delivered_STATE
import com.example.kleine.util.Constants.Companion.ORDER_PLACED_STATE
import com.example.kleine.util.Constants.Companion.ORDER_SHIPPED_STATE
import com.example.kleine.viewmodel.shopping.ShoppingViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderDetails : Fragment() {
    val TAG = "OrderDetails"
    val args by navArgs<OrderDetailsArgs>()
    private lateinit var binding: FragmentOrderDetailsBinding
    private lateinit var viewModel: ShoppingViewModel
    private lateinit var productsAdapter: CartRecyclerAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = (activity as ShoppingActivity).viewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOrderDetailsBinding.inflate(inflater)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val materialId = arguments?.getString("materialDocId") ?: ""
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        // Reference to the Firestore database
        val firestore = FirebaseFirestore.getInstance()

        binding.imgCloseOrder.setOnClickListener{
            findNavController().navigate(R.id.action_orderDetails_to_profileFragment)
        }

        // Initialize the RecyclerView
        setupRecyclerview()

        // Fetch the user's enrolled courses
        firestore.collection("Materials").document(materialId).collection("Courses")
            .get()
            .addOnSuccessListener { coursesQuerySnapshot ->
                val courseDocuments = coursesQuerySnapshot.documents.mapNotNull { document ->
                    document.toObject(CourseDocument::class.java)
                }
                // Update the adapter with the fetched CourseDocuments
                productsAdapter.submitList(courseDocuments)
            }
            .addOnFailureListener { exception ->
                // Handle the error appropriately
                Log.e(TAG, "Error fetching courses", exception)
            }


        //rating view
        if (userId != "" && materialId != "") {
            val db = FirebaseFirestore.getInstance()
            db.collection("Comments")
                .whereEqualTo("userId", userId)
                .whereEqualTo("materialId", materialId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    binding.ratingBtn.isEnabled = querySnapshot.documents.isEmpty()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error checking comments: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        val showReviewDialogButton: Button = binding.ratingBtn
        showReviewDialogButton.setOnClickListener {
            showReviewDialog(materialId)
        }

        binding.btnStartQuiz.setOnClickListener {
            val action = OrderDetailsDirections.actionOrderDetailsToQuizFragment(materialId)
            Log.d(TAG, "Navigating to QuizFragment with Material ID: ${materialId}") // Log the navigation
            findNavController().navigate(action)
        }
    }


    private fun downloadDocument(documentUrl: String) {
        val uri = Uri.parse(documentUrl)
        val request = DownloadManager.Request(uri)

        // Set the MIME type of the file
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(documentUrl))
        request.setMimeType(mimeType)

        // Set the destination path and file name
        val fileName = "downloaded_file.${MimeTypeMap.getFileExtensionFromUrl(documentUrl)}"
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        // Set the notification to be visible and shows the download progress
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val downloadManager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }


    private fun setupRecyclerview() {
        productsAdapter = CartRecyclerAdapter().apply {
            onDocumentDownloadClick = { documentUrl ->
                downloadDocument(documentUrl)
            }
        }
        binding.rvProducts.apply {
            adapter = productsAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(VerticalSpacingItemDecorator(23))
        }
    }




    //rating view
    private fun showReviewDialog(materialId: String?) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_review, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val ratingBar: RatingBar = dialogView.findViewById(R.id.rating_bar)
        val etReview: EditText = dialogView.findViewById(R.id.et_review)
        val btnSubmit: Button = dialogView.findViewById(R.id.btn_submit)

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating
            val review = etReview.text.toString()

            // Validate the input
            if (rating == 0f || review.isEmpty()) {
                val errorMessage = buildString {
                    if (rating == 0f) append("• Rating cannot be empty.\n")
                    if (review.isEmpty()) append("• Review cannot be empty.\n")
                }
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            } else {
                binding.ratingBtn.isEnabled = false
                btnSubmit.isEnabled = false
                etReview.isEnabled = false
                ratingBar.isEnabled = false
                // Format the current time as a string
                val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                    Date()
                )

                // Get the current user ID
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                    Toast.makeText(requireContext(), "Error: User not signed in", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Create a Map object with the data to be added to Firestore
                val commentData = mapOf(
                    "comment" to review,
                    "commentDate" to currentTime,
                    "materialId" to materialId,
                    "rating" to rating.toDouble(),
                    "replyComment" to "",
                    "replyDate" to "",
                    "replyPartnerId" to "",
                    "userId" to userId
                )

                // Get a reference to the Firestore database
                val db = FirebaseFirestore.getInstance()

                // Add a new document to the "Comments" collection
                db.collection("Comments")
                    .add(commentData)
                    .addOnSuccessListener { commentRef ->
                        // Comment posted successfully
                        Toast.makeText(requireContext(), "Comment posted successfully", Toast.LENGTH_SHORT).show()

                        // Calculate the new average rating
                        db.collection("Comments")
                            .whereEqualTo("materialId", materialId)
                            .get()
                            .addOnSuccessListener { commentsSnapshot ->
                                val ratings = commentsSnapshot.documents.map { it.getDouble("rating") ?: 0.0 }
                                val averageRating = ratings.sum() / ratings.size

                                if (materialId != null) {
                                    db.collection("Materials")
                                        .document(materialId)
                                        .update("rating", averageRating)
                                        .addOnSuccessListener {
                                            // Average rating updated successfully
                                            Toast.makeText(requireContext(), "Average rating updated successfully", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            // Error updating average rating
                                            Toast.makeText(requireContext(), "Error updating average rating: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                // Error calculating average rating
                                Toast.makeText(requireContext(), "Error calculating average rating: ${e.message}", Toast.LENGTH_SHORT).show()
                            }

                        // Close the review dialog
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        // Error posting comment
                        Toast.makeText(requireContext(), "Error posting comment: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        dialog.show()
    }

}