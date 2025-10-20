package com.cbi.mobile_plantation.data.model

data class MissingPhotosResponse(
    val success: Boolean,
    val message: String,
    val searchCriteria: SearchCriteria,
    val summary: Summary,
    val missingPhotos: List<MissingPhoto>
)

data class SearchCriteria(
    val tanggal: String,
    val deptAbbr: String,
    val divisiAbbr: String?,
    val createdBy: Int?,
    val type: String
)

data class Summary(
    val totalRecords: Int,
    val totalPhotos: Int,
    val foundPhotos: Int,
    val missingPhotosCount: Int,
    val recordsWithMissingPhotos: Int
)

data class MissingPhoto(
    val filename: String,
    val fullPath: String,
    val recordId: Int,
    val tanggal: String,
    val deptAbbr: String,
    val divisiAbbr: String,
    val blokKode: String,
    val tphNomor: String,
    val ancak: Int,
    val createdName: String,
    val createdBy: Int,
    val type: String
)