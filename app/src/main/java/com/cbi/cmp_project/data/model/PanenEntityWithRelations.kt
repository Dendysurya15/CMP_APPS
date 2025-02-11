package com.cbi.cmp_project.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.cbi.markertph.data.model.BlokModel
import com.cbi.markertph.data.model.DeptModel
import com.cbi.markertph.data.model.DivisiModel
import com.cbi.markertph.data.model.TPHNewModel

data class PanenEntityWithRelations(
    @Embedded val panen: PanenEntity,

    @Relation(
        parentColumn = "tph_id",
        entityColumn = "id"
    )
    val tph: TPHNewModel?
)


