package com.cbi.mobile_plantation.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class PathWithInspectionTphRelations(
    @Embedded val path: InspectionPathModel,

    @Relation(
        parentColumn = "id",
        entityColumn = "id_path",
        entity = InspectionModel::class
    )
    val inspections: List<InspectionWithTph>
) {
    fun getPathId(): String = path.id

    fun getBlok(): String = inspections.firstOrNull()?.tph?.blok_kode ?: "-"

    fun getTotalData(): Int = inspections.size

    fun getCreatedDate(): String = inspections.firstOrNull()?.inspection?.created_date ?: "-"
}