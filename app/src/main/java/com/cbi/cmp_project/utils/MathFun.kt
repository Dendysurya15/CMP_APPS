package com.cbi.cmp_project.utils

import org.json.JSONObject
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

    fun roundCeil(d: Float, decimalPlace: Int): Int? {
        var bd = BigDecimal(d.toString())
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_CEILING)
        return try {
            bd.toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun roundFloor(d: Float, decimalPlace: Int): Int? {
        var bd = BigDecimal(d.toString())
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_FLOOR)
        return try {
            bd.toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun roundUp(d: Float): Int? {
        var bd = try {
            BigDecimal(d.toString())
        } catch (e: Exception) {
            BigDecimal("0")
        }
        bd = bd.setScale(0, BigDecimal.ROUND_UP)
        return try {
            bd.toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun taksasiForm(akp: Float, stringLuas: String, stringSph: String, stringBjr: String): Float {
        val f = try {
            round(akp * toFloat(stringLuas) * toFloat(stringSph) * toFloat(stringBjr) / 100f, 2)!!
        } catch (e: Exception) {
            0f
        }
        return f
    }

    fun hkForm(taksasi: Float, stringOutput: String): Float {
        val f = try { val a = round(taksasi / toFloat(stringOutput), 0)!!
            roundUp(a)
        } catch (e: Exception) {
            0f
        }
        return f as Float
    }

    fun luasHkForm(luas: Float, hkFloat: Float): Float {
        val f = try {
            round(luas / hkFloat, 2)!!
        } catch (e: Exception) {
            0f
        }
        return f
    }

    fun toFloat(stringKeFloat: String): Float {
        val f = try {
            stringKeFloat.replace("," , ".").toFloat()
        } catch (e: Exception) {
            0f
        }
        return f
    }

    fun akpForm(jmlhjjg: String, jmlhpk: String): Float {
        val f = try {
            round(jmlhjjg.toFloat() / jmlhpk.toFloat() * 100f, 2)!!
        } catch (e: java.lang.Exception) {
            0f
        }
        return f
    }

    fun arrayAdder(userDetail: JSONObject, int: Int):String{
        return try { userDetail.getString(int.toString()) }catch (e:Exception){ "null" }
    }

    fun arrayAdderDB(userDetail: JSONObject, int: Int, arrayString: Array<String>): String{
        return try{arrayString[userDetail.getString(int.toString()).toInt()]}catch (e: Exception){"null"}
    }
}