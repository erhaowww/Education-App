package com.example.kleine.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class Status {
    USERS, PARTNERS, ADMINS
}

@Parcelize
data class User(
    var firstName: String,
    var lastName: String,
    var email: String,
    var imagePath: String = "",
    var status: Status = Status.USERS
): Parcelable {

    constructor() : this("","","","")
}

data class UserDetails(
    val userName: String,
    val userImage: String?
)

