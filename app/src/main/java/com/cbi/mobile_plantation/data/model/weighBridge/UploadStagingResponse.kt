package com.cbi.mobile_plantation.data.model.weighBridge

import com.google.gson.annotations.SerializedName

data class UploadStagingResponse(
    @SerializedName("status") val status: Int, // 1 for success, 0 for failure
    @SerializedName("message") val message: Any,
    @SerializedName("reponse") val response: Any? // Adjust type if needed
)
