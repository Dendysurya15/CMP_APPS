package com.cbi.cmp_project.data.model.dataset

import com.google.gson.annotations.SerializedName

// In data/model/DatasetRequest.kt
data class DatasetRequest(
    @SerializedName("estate") val estate: Int? = null,
    @SerializedName("regional") val regional: Int? = null,
    @SerializedName("last_modified") val lastModified: String?,
    @SerializedName("dataset") val dataset: String
)