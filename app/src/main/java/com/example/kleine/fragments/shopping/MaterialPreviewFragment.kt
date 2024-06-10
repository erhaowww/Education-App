package com.example.kleine.fragments.shopping

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.kleine.R
import com.example.kleine.adapters.viewpager.ViewPager2Images
import com.example.kleine.databinding.FragmentProductPreviewBinding
import com.example.kleine.databinding.FragmentTempCommentBinding
import com.example.kleine.model.CommentWithUserDetails
import com.example.kleine.model.Enrollment
import com.example.kleine.model.Material
import com.example.kleine.viewmodel.comment.CommentViewModel
import com.example.kleine.viewmodel.user.UserViewModel
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage



class MaterialPreviewFragment : Fragment() {
    private var _binding: FragmentProductPreviewBinding? = null
    private val binding get() = _binding!!
    private var material: Material? = null

    private val viewPagerAdapter = ViewPager2Images()

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    //comment use
    private var areCommentsVisible = true
    val userViewModel: UserViewModel by viewModels()
    val commentViewModel: CommentViewModel by viewModels {
        CommentViewModel.CommentViewModelFactory(userViewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onEnrollClick()

        // Retrieve the passed argument
        material = arguments?.getParcelable("material")

        material?.let { mat ->
            binding.productModel = mat

            // Load the image using Glide
            Glide.with(this)
                .load(mat.imageUrl)
                .into(binding.materialImage)

        } ?: run {
            Log.e("MaterialPreviewFragment", "Material is null!")
        }

        //comment use
        val adapter = CommentsAdapter(listOf())
        binding.allMaterialComment.materialCommentData.adapter = adapter

        val materialId = material?.id
        commentViewModel.fetchComments(materialId.toString())

        commentViewModel.commentsWithUserDetails.observe(viewLifecycleOwner, Observer { commentsWithUserDetails ->
            val sortedComments = commentsWithUserDetails.sortedByDescending {
                it.comment.commentDate
            }

            adapter.setData(sortedComments)
        })

        binding.allMaterialComment.commentTitle.setOnClickListener {
            toggleComments()
        }
        binding.allMaterialComment.downArrowComment.setOnClickListener {
            toggleComments()
        }
    }



    private fun onEnrollClick() {
        binding.btnEnroll.setOnClickListener {
            Log.d("MaterialPreviewFragment", "Button Clicked")

            // Get the current user ID
            val userId = firebaseAuth.currentUser?.uid ?: run {
                Toast.makeText(context, "User not logged in!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get the selected material ID
            val materialId = material?.id ?: run {
                Toast.makeText(context, "Material ID is null!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if the user has already enrolled
            firestore.collection("enrollments")
                .whereEqualTo("userId", userId)
                .whereEqualTo("materialId", materialId)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        Toast.makeText(context, "You have already enrolled in this course!", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // If the user hasn't enrolled yet, proceed with the enrollment process
                    val materialRef = firestore.collection("Materials").document(materialId)

                    firestore.runTransaction { transaction ->
                        // Get the current state of the material
                        val snapshot = transaction.get(materialRef)

                        // Increment the enroll field value
                        val newEnrollValue = snapshot.getLong("enroll")?.plus(1) ?: 1L

                        // Update the enroll field
                        transaction.update(materialRef, "enroll", newEnrollValue)

                        // Create a new Enrollment object
                        val enrollment = Enrollment(userId = userId, materialId = materialId)

                        // Add the enrollment document and return the newEnrollValue for further use if needed
                        transaction.set(firestore.collection("enrollments").document(), enrollment)
                        newEnrollValue
                    }.addOnSuccessListener {
                        Toast.makeText(context, "Successfully enrolled in the course!", Toast.LENGTH_SHORT).show()

                        // Navigate back to HomeFragment
                        findNavController().navigate(R.id.action_materialDetailsFragment_to_homeFragment)
                    }.addOnFailureListener { exception ->
                        Log.w("MaterialPreviewFragment", "Error adding document", exception)
                        Toast.makeText(context, "Error enrolling in the course!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("MaterialPreviewFragment", "Error checking enrollment", exception)
                    Toast.makeText(context, "Error checking enrollment status!", Toast.LENGTH_SHORT).show()
                }
        }
    }






    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    //comment use
    private fun toggleComments() {
        // Toggle the visibility of the RecyclerView
        if (areCommentsVisible) {
            binding.allMaterialComment.materialCommentData.visibility = View.GONE
            binding.allMaterialComment.downArrowComment.animate().rotation(0f).setDuration(300).start() // Rotate to initial position
        } else {
            binding.allMaterialComment.materialCommentData.visibility = View.VISIBLE
            binding.allMaterialComment.downArrowComment.animate().rotation(180f).setDuration(300).start() // Rotate 180 degrees
        }
        // Update the state
        areCommentsVisible = !areCommentsVisible
    }

    inner class CommentsAdapter(
        private var commentsWithUserDetails: List<CommentWithUserDetails>
    ) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {
        inner class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val commentTextView: TextView = view.findViewById(R.id.userComment)
            val commentDateTextView: TextView = view.findViewById(R.id.userCommentDate)
            val userNameTextView: TextView = view.findViewById(R.id.commentUserTextTitle)
            val userImageView: ImageView = view.findViewById(R.id.userImage)
            val replyTextView: TextView = view.findViewById(R.id.userReplyText)
            val replyDateTextView: TextView = view.findViewById(R.id.replyCommentDate)
            val partnerNameTextView: TextView = view.findViewById(R.id.commentReplyUserTextTitle)
            val partnerImageView: ImageView = view.findViewById(R.id.partnerImage)
            val ratingTextView: TextView = view.findViewById(R.id.rating)
            val replyCommentDate: TextView = view.findViewById(R.id.replyCommentDate)
            val arrow: MaterialCardView = view.findViewById(R.id.firstReplyCommentArrowImg)
            val firstReplyCommentImg: MaterialCardView = view.findViewById(R.id.firstReplyCommentImg)
            val userReplyCommentCard: ConstraintLayout = view.findViewById(R.id.userReplyCommentCard)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_material_comment, parent, false)
            return CommentViewHolder(view)
        }

        override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
            val commentWithUserDetails = commentsWithUserDetails[position]
            val comment = commentWithUserDetails.comment
            holder.commentTextView.text = comment.comment
            holder.commentDateTextView.text = comment.commentDate
            holder.userNameTextView.text = commentWithUserDetails.userName
            holder.ratingTextView.text = comment.rating.toString()
            if (commentWithUserDetails.userImage != null) {
                val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(commentWithUserDetails.userImage)
                storageReference.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(holder.userImageView.context)
                        .load(uri.toString())
                        .into(holder.userImageView)
                }
            }
            if (commentWithUserDetails.partnerName != null) {
                holder.replyTextView.text = comment.replyComment
                holder.replyDateTextView.text = comment.replyDate
                holder.partnerNameTextView.text = commentWithUserDetails.partnerName
                if (commentWithUserDetails.partnerImage != null) {
                    val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(commentWithUserDetails.partnerImage)
                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        Glide.with(holder.partnerImageView.context)
                            .load(uri.toString())
                            .into(holder.partnerImageView)
                    }
                }
            } else {
                holder.arrow.visibility = View.GONE
                holder.firstReplyCommentImg.visibility = View.GONE
                holder.userReplyCommentCard.visibility = View.GONE
                holder.replyCommentDate.visibility = View.GONE
            }
        }




        override fun getItemCount(): Int {
            return commentsWithUserDetails.size
        }

        fun setData(newCommentsWithUserDetails: List<CommentWithUserDetails>) {
            this.commentsWithUserDetails = newCommentsWithUserDetails
            notifyDataSetChanged()
        }
    }
}
