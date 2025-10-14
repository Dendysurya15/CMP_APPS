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

data class DownloadMapProgressResponse(
    val success: Boolean,
    val data: DownloadMapProgressData
)

data class DownloadMapProgressData(
    val downloadId: String,
    val estateName: String,
    val estateAbbr: String,
    val status: String,
    val progress: Int,
    val totalTiles: Int,
    val downloadedTiles: Int,
    val failedTiles: Int,
    val totalSize: String,
    val sizeBytesRaw: Long,
    val chunkSize: String,
    val chunksCount: Int,
    val createdAt: String,
    val updatedAt: String,
    val error: String?,
    val chunks: List<ChunkItem>
)

data class ChunkItem(
    val index: Int,
    val filename: String,
    val size: String,
    val downloadUrl: String,
    val checksum: String
)