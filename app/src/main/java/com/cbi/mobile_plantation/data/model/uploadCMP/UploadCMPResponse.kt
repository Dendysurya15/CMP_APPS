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

data class UploadResults(
    val processed: Int,
    val created: Int,
    val updated: Int,
    val errors: Int,
    val skipped: Int
)


data class DuplicateData(
    @SerializedName("id_tph") val idTph: Int,
    @SerializedName("datetime") val datetime: String
)

data class CheckDuplicateResponse(
    @SerializedName("status") val status: String,
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("espb_duplicates") val espbDuplicates: List<String>? = null,
    @SerializedName("tph_duplicates") val tphDuplicates: List<DuplicateData>? = null,
    @SerializedName("tph_new_records") val tphNewRecords: List<DuplicateData>? = null,
    @SerializedName("message") val message: String? = null
)

data class DuplicateInfo(
    val blokKode: String,        // Block code (e.g., "A01", "B05")
    val tphNomor: String,        // TPH number (e.g., "123" or "ID 135709")
    val formattedDate: String,   // Formatted date (e.g., "15 Jan 2026 10:19:47")
    val rawDate: String,         // Original date from API (e.g., "2026-01-15 10:19:47")
    val idTph: Int,              // TPH ID number (e.g., 135709)
    val jjgCount: String,        // JJG count (e.g., "25")
    val type: String             // Type: "ESPB" or "TPH"
)