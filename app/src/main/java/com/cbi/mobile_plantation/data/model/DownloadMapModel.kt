package com.cbi.mobile_plantation.data.model

data class DownloadMapResponse(
    val success: Boolean,
    val data: DownloadMapData
)

data class DownloadMapData(
    val downloads: List<DownloadMapItem>
)

data class DownloadMapItem(
    val downloadId: String,
    val estateName: String,
    val estateAbbr: String,
    val status: String,
    val progress: Int,
    val totalTiles: Int,
    val downloadedTiles: Int,
    val totalSize: String,
    val createdAt: String,
    val updatedAt: String
)