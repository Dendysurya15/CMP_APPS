package com.cbi.cmp_project.data.model.uploadCMP

data class UploadCMPResponse(
    val success: Boolean,
    val message: String,
    val trackingId: Int,
    val status: Int,
    val tanggal_upload: String,
    val nama_file: String
)
