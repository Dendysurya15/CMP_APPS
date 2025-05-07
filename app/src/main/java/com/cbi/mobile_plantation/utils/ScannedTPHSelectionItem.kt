package com.cbi.mobile_plantation.utils


data class ScannedTPHSelectionItem(
    val id: Int,
    val number: String,
    val blockCode: String,
    val distance: Float,
    val isAlreadySelected: Boolean,
    val selectionCount: Int,
    val canBeSelectedAgain: Boolean,
    val isWithinRange: Boolean = true,
    val jenisTPHId: String = "1" // Added jenisTPHId with default value
)