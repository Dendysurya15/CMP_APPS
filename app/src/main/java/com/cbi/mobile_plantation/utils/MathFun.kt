package com.cbi.mobile_plantation.utils

import java.math.BigDecimal

class MathFun(){

    fun round(d: Float, decimalPlace: Int): Float? {
        var bd = try {
            BigDecimal(d.toString())
        } catch (e: Exception) {
            BigDecimal("0")
        }
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP)
        return try {
            bd.toFloat()
        } catch (e: Exception) {
            0f
        }
    }



}