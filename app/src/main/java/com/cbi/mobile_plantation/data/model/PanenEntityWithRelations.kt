package com.cbi.mobile_plantation.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.cbi.markertph.data.model.TPHNewModel

data class PanenEntityWithRelations(
    @Embedded val panen: PanenEntity,

    @Relation(
        parentColumn = "tph_id",
        entityColumn = "id"
    )
    val tph: TPHNewModel?
)


