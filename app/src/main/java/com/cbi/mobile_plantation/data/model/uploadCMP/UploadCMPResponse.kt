package com.cbi.mobile_plantation.data.model.uploadCMP

import com.google.gson.annotations.SerializedName

data class UploadCMPResponse(
    @SerializedName("tracking_id") val tracking_id: Int,
    val success: Boolean,
    val message: String,
    @SerializedName("uploadedParts") val uploadedParts: Int,
    @SerializedName("totalParts") val totalParts: Int,
    val uuid: String,
    val status: String,
    val statusCode: Int,
    @SerializedName("processingTriggered") val processingTriggered: Boolean,
    @SerializedName("fileName") val fileName: String,
    @SerializedName("fileSize") val fileSize: Int,
    @SerializedName("tanggal_upload") val tanggal_upload: String
)


// Data classes for the response structure
data class CheckZipServerResponse(
    val success: Boolean,
    val uuid: String,
    val uploadStatus: String,
    val statusCode: Int,
    val message: String,
    val uploadedParts: Int,
    val totalParts: Int,
    val parts: List<PartInfo>,
    val missingParts: List<Int>?,
    val totalSize: Long,
    val tanggal_upload: String
)

data class PartInfo(
    val part: Int,
    val name: String,
    val size: Long
)


