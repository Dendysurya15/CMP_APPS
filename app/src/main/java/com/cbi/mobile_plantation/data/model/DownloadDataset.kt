package com.cbi.mobile_plantation.data.model

import com.google.gson.annotations.SerializedName

data class DownloadDataset(
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: String  // URL or path of the downloaded file
)