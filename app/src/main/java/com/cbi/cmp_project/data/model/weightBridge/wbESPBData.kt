package com.cbi.cmp_project.data.model.weightBridge

import com.google.gson.annotations.SerializedName

data class wbQRData(
    @SerializedName("espb") val espb: wbESPBData,
    @SerializedName("tph_0") val tph0: String?,
    @SerializedName("tph_1") val tph1: String?
)

data class wbESPBData(
    @SerializedName("blok_jjg") val blokJjg: String,
    @SerializedName("nopol") val nopol: String,
    @SerializedName("driver") val driver: String,
    @SerializedName("pemuat") val pemuat: Int,
    @SerializedName("transporter") val transporter: Int,
    @SerializedName("mill_id") val millId: Int,
    @SerializedName("created_by_id") val createdById: Int,
    @SerializedName("no_espb") val noEspb: String,
    @SerializedName("created_at") val createdAt: String
)