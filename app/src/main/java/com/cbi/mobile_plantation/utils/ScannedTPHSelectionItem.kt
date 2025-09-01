package com.cbi.mobile_plantation.utils


data class ScannedTPHSelectionItem(
    val id: Int,
    val number: String,
    val blockCode: String,
    val divisiCode: String?=null,
    val deptCode: String?=null,
    val jml_pokok_ha: Int?=0,
    val distance: Float,
    val isAlreadySelected: Boolean,
    val selectionCount: Int,
    val canBeSelectedAgain: Boolean,
    val isWithinRange: Boolean,
    val jenisTPHId: String,
    val customLimit: String? = null
)