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

    // Relation between PanenEntity and TPHNewModel
    @Relation(
        parentColumn = "tph_id",  // Reference to tph_id in PanenEntity
        entityColumn = "id"  // Reference to id in TPHNewModel
    )
    val tphges: TPHNewModel?,

    // Relation between TPHNewModel and DeptModel (dept column in TPHNewModel)
    @Relation(
        entity = DeptModel::class,
        parentColumn = "id",  // Reference to id in TPHNewModel
        entityColumn = "id",  // Reference to id in DeptModel
        associateBy = Junction(
            value = TPHNewModel::class,
            parentColumn = "id",  // Reference to id in TPHNewModel
            entityColumn = "dept"  // Reference to dept in TPHNewModel
        )
    )
    val department: DeptModel?,

    // Relation between TPHNewModel and DivisiModel (divisi column in TPHNewModel)
    @Relation(
        entity = DivisiModel::class,
        parentColumn = "id",  // Reference to id in TPHNewModel
        entityColumn = "id",  // Reference to id in DivisiModel
        associateBy = Junction(
            value = TPHNewModel::class,
            parentColumn = "id",  // Reference to id in TPHNewModel
            entityColumn = "divisi"  // Reference to divisi in TPHNewModel
        )
    )
    val division: DivisiModel?,

    // Relation between TPHNewModel and BlokModel (blok column in TPHNewModel)
    @Relation(
        entity = BlokModel::class,
        parentColumn = "id",  // Reference to id in TPHNewModel
        entityColumn = "id",  // Reference to id in BlokModel
        associateBy = Junction(
            value = TPHNewModel::class,
            parentColumn = "id",  // Reference to id in TPHNewModel
            entityColumn = "blok"  // Reference to blok in TPHNewModel
        )
    )
    val block: BlokModel?
)


