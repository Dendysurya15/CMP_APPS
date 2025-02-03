package com.cbi.cmp_project.data.repository

import android.content.ContentValues
import android.content.Context
import com.cbi.cmp_project.data.model.PanenTBSModel

class PanenTBSRepository(context: Context) {

//    private val databaseHelper: DatabaseHelper = DatabaseHelper(context)
//
//    fun insertPanenTBSRepo(data: PanenTBSModel): Boolean {
//        val db = databaseHelper.writableDatabase
//        val values = ContentValues().apply {
//            put(DatabaseHelper.KEY_USERID, data.user_id)
//            put(DatabaseHelper.KEY_TANGGAL, data.tanggal)
//            put(DatabaseHelper.KEY_NAME, data.name)
//            put(DatabaseHelper.KEY_ESTATE, data.estate)
//            put(DatabaseHelper.KEY_IDESTATE, data.id_estate)
//            put(DatabaseHelper.KEY_AFDELING, data.afdeling)
//            put(DatabaseHelper.KEY_IDAFDELING, data.id_afdeling)
//            put(DatabaseHelper.KEY_BLOK, data.blok)
//            put(DatabaseHelper.KEY_IDBLOK, data.id_blok)
//            put(DatabaseHelper.KEY_TTANAM, data.tahun_tanam)
//            put(DatabaseHelper.KEY_IDTANAM, data.id_tt)
//            put(DatabaseHelper.KEY_ANCAK, data.ancak)
//            put(DatabaseHelper.KEY_IDANCAK, data.id_ancak)
//            put(DatabaseHelper.KEY_TPH, data.tph)
//            put(DatabaseHelper.KEY_IDTPH, data.id_tph)
//            put(DatabaseHelper.KEY_JENISPANEN, data.jenis_panen)
//            put(DatabaseHelper.KEY_LISTPEMANEN, data.list_pemanen)
//            put(DatabaseHelper.KEY_LISTIDPEMANEN, data.list_idpemanen)
//            put(DatabaseHelper.KEY_TBS, data.tbs)
//            put(DatabaseHelper.KEY_TBSMENTAH, data.tbs_mentah)
//            put(DatabaseHelper.KEY_TBSLEWAT, data.tbs_lewatmasak)
//            put(DatabaseHelper.KEY_TKS, data.tks)
//            put(DatabaseHelper.KEY_ABNORMAL, data.abnormal)
//            put(DatabaseHelper.KEY_TIKUS, data.tikus)
//            put(DatabaseHelper.KEY_TANGKAI, data.tangkai_panjang)
//            put(DatabaseHelper.KEY_VCUT, data.vcut)
//            put(DatabaseHelper.KEY_MASAK, data.tbs_masak)
//            put(DatabaseHelper.KEY_DIBAYAR, data.tbs_dibayar)
//            put(DatabaseHelper.KEY_KIRIM, data.tbs_kirim)
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