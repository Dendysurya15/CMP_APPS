package com.cbi.mobile_plantation.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.cbi.mobile_plantation.data.model.TPHNewModel

data class InspectionWithTph(
    @Embedded
    val inspection: InspectionModel,

    @Relation(
        parentColumn = "tph_id",
        entityColumn = "id"
    )
    val tph: TPHNewModel
)