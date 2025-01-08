package com.cbi.cmp_project.data.model

import androidx.room.Ignore

data class TPHModel(
    val id: Int,
    val CompanyCode: Int,
    val Regional: Int,
    val BUnitCode: Int,
    val DivisionCode:Int,
    val FieldCode:Int,
    val planting_year:Int,
    val ancak:Int,
    val tph:String,
)