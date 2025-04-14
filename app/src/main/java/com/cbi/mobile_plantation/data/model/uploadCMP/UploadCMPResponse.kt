package com.cbi.mobile_plantation.data.model.uploadCMP

import com.google.gson.annotations.SerializedName

data class UploadCMPResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("uploadedParts") val uploadedParts: Int,
    @SerializedName("totalParts") val totalParts: Int,
    val uuid: String,
    val status: Int,
    @SerializedName("processingTriggered") val processingTriggered: Boolean,
    @SerializedName("fileName") val fileName: String,
    @SerializedName("fileSize") val fileSize: Int,
    @SerializedName("tanggal_upload") val tanggal_upload: String
)


