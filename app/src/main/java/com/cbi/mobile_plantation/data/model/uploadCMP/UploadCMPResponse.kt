package com.cbi.mobile_plantation.data.model.uploadCMP

import com.google.gson.annotations.SerializedName

data class UploadCMPResponse(
    @SerializedName("trackingId") val trackingId: Int,
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

data class UploadWBCMPResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("trackingId") val trackingId: String,
    @SerializedName("uploadedParts") val uploadedParts: Int,
    val status: Int,
    @SerializedName("tanggal_upload") val tanggal_upload: String,
    @SerializedName("nama_file") val nama_file: String
)


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
    val tanggal_upload: String,
    val processingTriggered: Boolean? = null,
    val processingStartedAt: String? = null,
    val processingCompletedAt: String? = null,
    val trackingInfo: List<TrackingInfo>? = null
)

data class PartInfo(
    val part: Int,
    val name: String,
    val size: Long
)

data class TrackingInfo(
    val trackingId: Int,
    val part: Int,
    val status: Int,
    val statusText: String,
    val message: String? = null,
    val fileName: String
)

