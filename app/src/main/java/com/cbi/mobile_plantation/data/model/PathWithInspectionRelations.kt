package com.cbi.mobile_plantation.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.cbi.mobile_plantation.data.model.InspectionModel
import com.cbi.mobile_plantation.data.model.InspectionPathModel

data class PathWithInspectionRelations(
    @Embedded val path: InspectionPathModel,

    @Relation(
        parentColumn = "id",
        entityColumn = "id_path"
    )
    val inspections: List<InspectionModel>
)