package com.cbi.mobile_plantation.data.model.uploadCMP

import com.google.gson.JsonObject
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

data class PhotoUploadResponse(
    val success: Boolean,
    val message: String,
    val data: PhotoUploadData
)

data class PhotoUploadData(
    val success: Boolean,
    val total: Int,
    val successful: Int,
    val failed: Int,
    val results: List<PhotoResult>
)

data class PhotoResult(
    val success: Boolean,
    val fileName: String,
    val originalName: String,
    val size: Long,
    val path: String,
    val datasetType: String,
    val uploadDate: String
)


data class checkStatusUploadedData(
    val success: Boolean,
    val data: List<StatusData>
)

data class StatusData(
    val id: Int,
    val nama_file: String,
    val tanggal_upload: String,
    val status: Int,
    val statusText: String,
    val message: String,
    val created_by: Int
)

data class UploadV3Response(
    val success: Boolean,
    val message: String,
    @SerializedName("trackingId") val trackingId: Int,
    val status: Int,
    @SerializedName("tanggal_upload") val tanggal_upload: String,
    @SerializedName("nama_file") val nama_file: String,
    val results: UploadResults?,
    val type: String,
    val imageFullPath: List<String>? = emptyList(),
    val imageName: List<String>? = emptyList(),
    val table_ids: String? = null
)
data class UploadResults(
    val processed: Int,
    val created: Int,
    val updated: Int,
    val errors: Int,
    val skipped: Int,
    val skipErrorDetails: List<SkipErrorDetail>? = null
)


data class SkipErrorDetail(
    val index: Int,
    val tph: String,
    val tanggal: String,
    @SerializedName("created_date") val createdDate: String,
    val reason: String,
    val data: JsonObject? = null  // or create another data class if you need specific fields
)

data class CheckDuplicateResponse(
    @SerializedName("status") val status: String,
    @SerializedName("processed") val processed: Int,
    @SerializedName("duplicates") val duplicates: List<DuplicateData>?
)

data class DuplicateData(
    @SerializedName("id_tph") val idTph: Int,
    @SerializedName("datetime") val datetime: String
)
