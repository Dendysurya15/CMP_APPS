package com.cbi.mobile_plantation.data.model.uploadCMP

data class UploadCMPResponse(
    val success: Boolean,
    val message: String,
    val trackingId: Int,
    val status: Int,
    val tanggal_upload: String,
    val nama_file: String
)


