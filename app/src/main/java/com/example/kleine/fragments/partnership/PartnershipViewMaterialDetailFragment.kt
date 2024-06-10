package com.example.kleine.fragments.partnership

import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kleine.R
import com.example.kleine.databinding.CommentViewDetailBinding
import com.example.kleine.databinding.FragmentPartnershipViewMaterialDetailBinding
import com.example.kleine.databinding.QuizViewDetailBinding
import com.example.kleine.model.CommentWithUserDetails
import com.example.kleine.resource.NetworkReceiver
import com.example.kleine.viewmodel.comment.CommentViewModel
import com.example.kleine.viewmodel.material.MaterialViewModel
import com.example.kleine.viewmodel.user.UserViewModel
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.w3c.dom.Text

data class QuizData(
    val id: String = "",
    val userImage: String = "",
    val username: String = "",
    val score: String = "",
    val materialId: String = "",
    val setName: String = ""
)

class PartnershipViewMaterialDetailFragment : Fragment(){
    val TAG = "PartnershipViewMaterialDetailFragment"
    private lateinit var binding: FragmentPartnershipViewMaterialDetailBinding
    private val materialViewModel: MaterialViewModel by viewModels()
    val db = FirebaseFirestore.getInstance()
    private lateinit var commentAdapter: CommentsAdapter
    private lateinit var quizAdapter: QuizAdapter
    private val quizDataLiveData = MutableLiveData<List<QuizData>>()
    val commentViewModel: CommentViewModel by viewModels {
        CommentViewModel.CommentViewModelFactory(userViewModel)
    }
    val userViewModel: UserViewModel by viewModels()
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
        binding = FragmentPartnershipViewMaterialDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val quizLayoutBinding = QuizViewDetailBinding.bind(binding.root)
        val commentLayoutBinding = CommentViewDetailBinding.bind(binding.root)

        commentAdapter = CommentsAdapter(listOf())
        quizAdapter = QuizAdapter(listOf())

        commentLayoutBinding.commentData.adapter = commentAdapter
        quizLayoutBinding.quizData.adapter = quizAdapter



        val documentId = arguments?.getString("documentId") ?: return

        commentViewModel.fetchComments(documentId)
        commentViewModel.commentsWithUserDetails.observe(viewLifecycleOwner, Observer { commentsWithUserDetails ->
            val sortedComments = commentsWithUserDetails.sortedByDescending {
                it.comment.commentDate
            }
            commentAdapter.setData(sortedComments)
        })
        quizDataLiveData.observe(viewLifecycleOwner, Observer { quizData ->
            quizAdapter.setData(quizData)
        })

        fetchQuizData(documentId)
        materialViewModel.materialEngageData.observe(viewLifecycleOwner) { materialEngageData ->
            // Update UI with material engage data
            binding.textTitle.text = materialEngageData?.name ?: ""
            binding.viewNum.text = materialEngageData?.view.toString()
            binding.enrollNum.text = materialEngageData?.enroll.toString()
            binding.graduateNum.text = materialEngageData?.graduate.toString()

            if (materialEngageData?.imageUrl?.isNotEmpty() == true) {
                val storageReference = FirebaseStorage.getInstance()
                    .getReferenceFromUrl(materialEngageData?.imageUrl.toString())
                storageReference.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(binding.root.context)
                        .load(uri.toString())
                        .into(binding.viewHeaderBackground)
                }
            }
        }
        materialViewModel.fetchMaterialsEngageData(documentId)



        binding.quizLayout.downArrowQuiz.setOnClickListener {
            toggleVisibilityAndRotateArrow(
                binding.quizLayout.downArrowQuiz,
                binding.quizLayout.quizCard,
                binding.quizLayout.quizData
            )
        }
        binding.commentLayout.downArrowComment.setOnClickListener {
            toggleVisibilityAndRotateArrow(
                binding.commentLayout.downArrowComment,
                binding.commentLayout.commentCard,
                binding.commentLayout.commentData
            )
        }
    }

    private fun toggleVisibilityAndRotateArrow(arrow: ImageView, vararg views: View) {
        for (view in views) {
            view.visibility = if (view.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        arrow.animate().rotationBy(180f).start()
    }
    private fun fetchQuizData(documentId: String) {
        db.collectionGroup("quizHistory").whereEqualTo("materialId", documentId).get()
            .addOnSuccessListener { querySnapshot ->
                val allQuizData = mutableListOf<QuizData>()
                for (quizDocument in querySnapshot.documents) {
                    val userId = quizDocument.reference.parent.parent?.id ?: ""
                    userViewModel.fetchUserName(userId) { username ->
                        userViewModel.fetchUserImage(userId) { userImage ->
                            val quizMaterialId = quizDocument.getString("materialId") ?: ""
                            val quizId = quizDocument.getString("setId") ?: ""
                            val score = quizDocument.getString("score")?: ""
                            db.collection("Materials").document(quizMaterialId).collection("Sets").document(quizId).get()
                                .addOnSuccessListener { setDocument ->
                                    // Extract setName from setDocument
                                    val setName = setDocument.getString("setName") ?: ""
                                    // Create QuizData object with setName
                                    allQuizData.add(
                                        QuizData(
                                            id = quizId,
                                            userImage = userImage.toString(),
                                            username = username.toString(),
                                            score = score,
                                            materialId = quizMaterialId,
                                            setName = setName
                                        )
                                    )
                                    quizDataLiveData.postValue(allQuizData)
                                }
                        }
                    }
                }
            }
    }
    inner class QuizAdapter(
        private var quizDataList: List<QuizData>
    ) : RecyclerView.Adapter<QuizAdapter.QuizViewHolder>() {

        inner class QuizViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val userName: TextView = view.findViewById(R.id.userTextTitleQuiz)
            val userImage: ImageView = view.findViewById(R.id.userImageQuiz)
            val score: TextView = view.findViewById(R.id.quizScore)
            val quizPaper: TextView = view.findViewById(R.id.userQuizPaper)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_view_quiz_detail, parent, false)
            return QuizViewHolder(view)
        }

        override fun getItemCount(): Int {
            return quizDataList.size
        }


        override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
            val quizData = quizDataList[position]
            holder.userName.text = quizData.username
            binding.quizLayout.totalResponse.text = getItemCount().toString()
            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(quizData.userImage)
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(holder.userImage.context)
                    .load(uri.toString())
                    .into(holder.userImage)
                holder.score.text = quizData.score
                holder.quizPaper.text = "Quiz Paper:" + quizData.setName
            }
            val (passAbove80Percent, overallPassingPercentage) = calculatePassingRate(quizDataList)
            binding.quizLayout.pass.text = passAbove80Percent.toString()
            binding.quizLayout.passPercentage.text = String.format("%.0f%%", overallPassingPercentage.toDouble())
        }
        fun setData(newQuizDataList: List<QuizData>) {
            this.quizDataList = newQuizDataList
            notifyDataSetChanged()
        }
        fun calculatePassingRate(quizDataList: List<QuizData>): Pair<Double, Double> {
            var totalPassCount = 0
            var totalQuizCount = 0

            for (quizData in quizDataList) {
                // Parse the score as a fraction (e.g., "3/5")
                val scoreParts = quizData.score.split("/")
                if (scoreParts.size == 2) {
                    val correctAnswers = scoreParts[0].toDoubleOrNull()
                    val totalQuestions = scoreParts[1].toDoubleOrNull()

                    if (correctAnswers != null && totalQuestions != null && totalQuestions > 0) {
                        val passingRate = (correctAnswers / totalQuestions) * 100
                        if (passingRate >= 80) {
                            totalPassCount++
                        }
                        totalQuizCount++
                    }
                }
            }

            val overallPassingPercentage = if (totalQuizCount > 0) {
                (totalPassCount.toDouble() / totalQuizCount) * 100
            } else {
                0.0
            }

            return Pair(totalPassCount.toDouble(), overallPassingPercentage)
        }

    }
    inner class CommentsAdapter(
        private var commentsWithUserDetails: List<CommentWithUserDetails>
    ) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {
        inner class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val userCommentImage: ImageView = view.findViewById(R.id.userCommentImage)
            val commentUserTextTitle: TextView = view.findViewById(R.id.commentUserTextTitle)
            val commentUserDate: TextView = view.findViewById(R.id.commentUserDate)
            val userRating: TextView = view.findViewById(R.id.userRating)
            val userComment: TextView = view.findViewById(R.id.userComment)
            val commentReply: TextView = view.findViewById(R.id.commentReply)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_comment_detail, parent, false)
            return CommentViewHolder(view)
        }

        override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
            val commentWithUserDetails = commentsWithUserDetails[position]
            val comment = commentWithUserDetails.comment
            holder.commentUserTextTitle.text = commentWithUserDetails.userName
            holder.commentUserDate.text = comment.commentDate
            holder.userRating.text = comment.rating.toString()
            holder.userComment.text = comment.comment

            binding.commentLayout.totalComment.text = getItemCount().toString()
            val averageRating = calculateAverageRating()
            binding.commentLayout.aveRating.text = String.format("%.1f/5", averageRating)

            if (commentWithUserDetails.userImage != null) {
                val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(commentWithUserDetails.userImage)
                storageReference.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(holder.userCommentImage.context)
                        .load(uri.toString())
                        .into(holder.userCommentImage)
                }
            }
            if (comment.replyPartnerId != "") {
                // Set background color to grey and make it not clickable
                holder.commentReply.setBackgroundColor(Color.GRAY)
                holder.commentReply.isClickable = false
            } else {
                // Set background color to green and make it clickable
                holder.commentReply.setBackgroundColor(Color.GREEN)
                holder.commentReply.isClickable = true
                holder.commentReply.setOnClickListener {
                    if (isNetworkAvailable) {
                        val commentId = comment.id
                        if (commentId != "") {
                            val bundle = Bundle()
                            bundle.putString("commentDocumentId", commentId)
                            findNavController().navigate(R.id.action_partnershipViewMaterialDetailFragment_to_replyCommentFragment, bundle)
                        }
                    } else {
                        showNoInternetDialog()
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return commentsWithUserDetails.size
        }
        fun setData(newCommentsWithUserDetails: List<CommentWithUserDetails>) {
            this.commentsWithUserDetails = newCommentsWithUserDetails
            notifyDataSetChanged()
        }
        fun calculateAverageRating(): Double {
            var totalRating = 0L
            for (commentWithUserDetails in commentsWithUserDetails) {
                totalRating += commentWithUserDetails.comment.rating
            }

            if (commentsWithUserDetails.isNotEmpty()) {
                return totalRating.toDouble() / commentsWithUserDetails.size
            }

            return 0.0
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
