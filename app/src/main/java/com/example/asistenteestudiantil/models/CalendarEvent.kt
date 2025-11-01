package com.example.asistenteestudiantil.models

import com.google.gson.annotations.SerializedName

data class CalendarEvent(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("startDate") val startDate: String = "",
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("type") val type: String = "", // "academico", "evento", "feriado", etc.
    @SerializedName("isImportant") val isImportant: Boolean = false
)

