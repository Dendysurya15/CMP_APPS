package com.cbi.mobile_plantation.data.model.pemanenFace

import com.google.gson.annotations.SerializedName

data class FaceUploadRequest(
    @SerializedName("karyawan_id") val karyawanId: Int,
    val nik: String,
    val nama: String,
    @SerializedName("kemandoran_nama") val kemandoranNama: String,
    val embedding: String,
    @SerializedName("updated_at") val updatedAt: String,
    val dept: Int? = null,
    val company: Int? = null
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
    @SerializedName("karyawan_id") val karyawanId: Int? = null,
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
    @SerializedName("karyawan_id") val karyawanId: Int,
    val nik: String,
    val nama: String,
    @SerializedName("kemandoran_nama") val kemandoranNama: String? = "",
    val embedding: String,
    @SerializedName("model_id") val modelId: String? = null,
    val dept: Int? = null,
    val company: Int? = null,
    @SerializedName("updated_at") val updatedAt: String
)
