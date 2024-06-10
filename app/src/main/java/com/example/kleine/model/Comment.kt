package com.example.kleine.model

data class Comment(
    var id: String = "",
    val comment: String ="",
    val commentDate: String="",
    val materialId: String="",
    val replyComment: String?="",
    val replyDate: String?="",
    val replyPartnerId: String?="",
    val userId: String="",
    val rating: Long=0
)

data class CommentWithUserDetails(
    val comment: Comment,
    val userName: String?,
    val userImage: String?,
    val partnerName: String?,
    val partnerImage: String?
)