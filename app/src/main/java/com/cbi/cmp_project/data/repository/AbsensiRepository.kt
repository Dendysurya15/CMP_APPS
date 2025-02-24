package com.cbi.cmp_project.data.repository

import android.content.Context

class AbsensiRepository(context: Context) {

    //    private val databaseHelper: DatabaseHelper = DatabaseHelper(context)
//
//    fun insertAbsensiRepo(data: AbsensiModel): Boolean {
//        val db = databaseHelper.writableDatabase
//        val values = ContentValues().apply {
//            put(DatabaseHelper.KEY_USERID, data.user_id)
//            put(DatabaseHelper.KEY_TANGGAL, data.tanggal)
//            put(DatabaseHelper.KEY_NAME, data.name)
//            put(DatabaseHelper.KEY_ESTATE, data.estate)
//            put(DatabaseHelper.KEY_IDESTATE, data.id_estate)
//            put(DatabaseHelper.KEY_AFDELING, data.afdeling)
//            put(DatabaseHelper.KEY_IDAFDELING, data.id_afdeling)
//            put(DatabaseHelper.KEY_LISTPEMANEN, data.list_pemanen)
//            put(DatabaseHelper.KEY_LISTIDPEMANEN, data.list_idpemanen)
//            put(DatabaseHelper.KEY_LAT, data.latitude)
//            put(DatabaseHelper.KEY_LON, data.longitude)
//            put(DatabaseHelper.KEY_PHOTO, data.foto)
//        }
//
//        val rowsAffected = db.insert(DatabaseHelper.DB_TABLE_PANEN_TBS, null, values)
////        db.close()
//
//        return rowsAffected > 0
//    }
}