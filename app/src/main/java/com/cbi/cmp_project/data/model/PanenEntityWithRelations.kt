package com.cbi.cmp_project.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.cbi.markertph.data.model.BlokModel
import com.cbi.markertph.data.model.DeptModel
import com.cbi.markertph.data.model.DivisiModel
import com.cbi.markertph.data.model.TPHNewModel

data class TPHWithBlok(
    @Embedded val tph: TPHNewModel,

    @Relation(
        parentColumn = "blok",
        entityColumn = "id"
    )
    val block: BlokModel?
)

data class PanenEntityWithRelations(
    @Embedded val panen: PanenEntity,

    @Relation(
        parentColumn = "tph_id",
        entityColumn = "id",
        entity = TPHNewModel::class
    )
    val tphWithBlok: TPHWithBlok?
)

