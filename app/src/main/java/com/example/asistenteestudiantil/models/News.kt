package com.example.asistenteestudiantil.models

import com.google.gson.annotations.SerializedName

data class News(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("content") val content: String = "",
    @SerializedName("author") val author: String = "",
    @SerializedName("publishedAt") val publishedAt: String = "",
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("category") val category: String = "",
    @SerializedName("link") val link: String? = null
)

