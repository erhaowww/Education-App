package com.example.kleine.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Enrollment(
    val userId: String = "", // ID of the user who has enrolled
    val materialId: String = "", // ID of the material/course in which the user has enrolled
    var archived: Boolean = false

): Parcelable
