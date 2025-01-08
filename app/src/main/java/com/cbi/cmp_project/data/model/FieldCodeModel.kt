package com.cbi.cmp_project.data.model

data class FieldCodeModel(
    val FieldCode:Int,
    val BUnitCode: Int,
    val DivisionCode:Int,
    val FieldName:String,
    val FieldNumber:String,
    val FieldLandArea:Double,
    val PlantingYear:Int,
    val IntialNoOfPlants:Int,
    val PlantsPerHectare:Int,
    val isMatured:String
)