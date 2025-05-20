package com.cbi.mobile_plantation.data.model.dataset

import com.google.gson.annotations.SerializedName

// In data/model/DatasetRequest.kt
data class DatasetRequest(
    @SerializedName("afdeling") val afdeling: String? = null,
    @SerializedName("estate") val estate: Int? = null,
    @SerializedName("estateAbbr") val estateAbbr: String? = null,
    @SerializedName("regional") val regional: Int? = null,
    @SerializedName("last_modified") val lastModified: String?,
    @SerializedName("dataset") val dataset: String,
    @SerializedName("data") val data: List<String>? = null,
    @SerializedName("isDownloadMasterTPHAsistensi") val isDownloadMasterTPHAsistensi: Boolean = false
)