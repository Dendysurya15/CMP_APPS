package com.cbi.mobile_plantation.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class MutuBuahWithRelations(
    @Embedded val mutu_buah: MutuBuahEntity,

    @Relation(
        parentColumn = "tph",
        entityColumn = "id"
    )
    val tph: TPHNewModel?
)
