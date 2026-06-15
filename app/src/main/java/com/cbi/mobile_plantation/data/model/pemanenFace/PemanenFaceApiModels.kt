package com.cbi.mobile_plantation.data.model.pemanenFace

import com.google.gson.annotations.SerializedName

data class FaceUploadRequest(
    val nik: String,
    val nama: String,
    val embedding: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class FaceUploadBatchRequest(
    val faces: List<FaceUploadRequest>
)

data class FaceApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)

data class FaceUploadBatchSummary(
    val saved: Int,
    val failed: Int,
    val total: Int,
    val results: List<FaceUploadBatchResult>? = null
)

data class FaceUploadBatchResult(
    val nik: String? = null,
    val status: String,
    val message: String? = null,
    val action: String? = null
)

data class FaceListData(
    val total: Int,
    val count: Int,
    @SerializedName("model_id") val modelId: String,
    val faces: List<FaceSyncItem>
)

data class FaceSyncItem(
    val nik: String,
    val nama: String,
    val embedding: String,
    @SerializedName("model_id") val modelId: String? = null,
    @SerializedName("updated_at") val updatedAt: String
)
