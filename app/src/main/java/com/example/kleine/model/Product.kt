package com.example.kleine.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.util.*
import kotlin.collections.HashMap


@Parcelize
data class Product(
    var id: Int,
    val productName: String? = "",
    val description: String? = "",
    val productCategory: String? = "",
    val newPrice: String? = "",
    val productPrice: String? = "", // Renamed from price to productPrice
    val productRate: Float? = 0f,  // Added productRate
    val seller: String? = "",
    val images: @RawValue HashMap<String, Any>? = null,
    val colors: @RawValue HashMap<String, Any>? = null,
    val sizes: @RawValue HashMap<String, Any>? = null,
    val orders: Int = 0,
    val offerTime: Date? = null,
    val sizeUnit: String? = null
) : Parcelable {
    constructor(
        id: Int,
        productName: String? = "",
        description: String? = "",
        productCategory: String? = "",
        productPrice: String? = "",
        seller: String? = "",
        images: HashMap<String, Any>,
        colors: HashMap<String, Any>,
        sizes: HashMap<String, Any>
    ) : this(id, productName, description, productCategory, null, productPrice, 0f, seller, images, colors, sizes, 0, null, null)

    constructor() : this(0, "", "", "", "", null, null, null)
}