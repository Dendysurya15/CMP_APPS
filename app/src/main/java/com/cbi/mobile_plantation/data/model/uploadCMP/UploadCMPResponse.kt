package com.cbi.mobile_plantation.data.model.uploadCMP

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName


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
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("espb_duplicates") val espbDuplicates: List<String>? = null,
    @SerializedName("tph_duplicates") val tphDuplicates: List<DuplicateData>? = null,
    @SerializedName("tph_new_records") val tphNewRecords: List<DuplicateData>? = null,
    @SerializedName("message") val message: String? = null
)

data class DuplicateData(
    @SerializedName("id_tph") val idTph: Int,
    @SerializedName("datetime") val datetime: String
)

data class UploadHarvestResponse(
    @SerializedName("status")
    val status: String,  // ← "success" bukan Int!

    @SerializedName("message")
    val message: String,

    @SerializedName("id")
    val id: Int,

    @SerializedName("noESPB")
    val noESPB: String
)