package com.cbi.mobile_plantation.data.model.uploadCMP


data class FetchStatusCMPResponse(
    val success: Boolean,
    val data: List<FetchResponseItem>
)

data class FetchResponseItem(
    val id: Int,
    val nama_file: String,
    val tanggal_upload: String,
    val status: Int,
    val message: String,
    val created_by: Int,
    val statusText: String
)
