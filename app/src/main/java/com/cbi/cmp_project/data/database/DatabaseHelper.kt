package com.cbi.cmp_project.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context):
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "cbi_cmp"
        const val DATABASE_VERSION = 2
        const val DB_ARCHIVE = "archive"

        val DB_TABLE_PANEN_TBS = "db_panen_tbs"
        val KEY_ID = "id"
        val KEY_USERID = "user_id"
        val KEY_TANGGAL = "tanggal"
        val KEY_NAME = "name"
        val KEY_ESTATE = "estate"
        val KEY_IDESTATE = "id_estate"
        val KEY_AFDELING = "afdeling"
        val KEY_IDAFDELING = "id_afdeling"
        val KEY_BLOK = "blok"
        val KEY_IDBLOK = "id_blok"
        val KEY_TTANAM = "tahun_tanam"
        val KEY_IDTANAM = "id_tt"
        val KEY_ANCAK = "ancak"
        val KEY_IDANCAK = "id_ancak"
        val KEY_TPH = "tph"
        val KEY_IDTPH = "id_tph"
        val KEY_JENISPANEN = "jenis_panen"
        val KEY_LISTPEMANEN = "list_pemanen"
        val KEY_LISTIDPEMANEN = "list_idpemanen"
        val KEY_TBS = "tbs"
        val KEY_TBSMENTAH = "tbs_mentah"
        val KEY_TBSLEWAT = "tbs_lewatmasak"
        val KEY_TKS = "tks"
        val KEY_ABNORMAL = "abnormal"
        val KEY_TIKUS = "tikus"
        val KEY_TANGKAI = "tangkai_panjang"
        val KEY_VCUT = "vcut"
        val KEY_MASAK = "tbs_masak"
        val KEY_DIBAYAR = "tbs_dibayar"
        val KEY_KIRIM = "tbs_kirim"
        val KEY_LAT = "latitude"
        val KEY_LON = "longitude"
        val KEY_PHOTO = "foto"


    }

    private val createTablePanenTBS = """
        CREATE TABLE IF NOT EXISTS $DB_TABLE_PANEN_TBS (
            $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $KEY_USERID INTEGER,
            $KEY_TANGGAL VARCHAR,
            $KEY_NAME VARCHAR,
            $KEY_ESTATE VARCHAR,
            $KEY_IDESTATE INTEGER,
            $KEY_AFDELING VARCHAR,
            $KEY_IDAFDELING INTEGER,
            $KEY_BLOK VARCHAR,
            $KEY_IDBLOK INTEGER,
            $KEY_TTANAM INTEGER,
            $KEY_IDTANAM INTEGER,
            $KEY_ANCAK VARCHAR,
            $KEY_IDANCAK INTEGER,
            $KEY_TPH VARCHAR,
            $KEY_IDTPH INTEGER,
            $KEY_JENISPANEN VARCHAR,
            $KEY_LISTPEMANEN VARCHAR,
            $KEY_LISTIDPEMANEN VARCHAR,
            $KEY_TBS INTEGER,
            $KEY_TBSMENTAH INTEGER,
            $KEY_TBSLEWAT INTEGER,
            $KEY_TKS INTEGER,
            $KEY_ABNORMAL INTEGER,
            $KEY_TIKUS INTEGER,
            $KEY_TANGKAI INTEGER,
            $KEY_VCUT INTEGER,
            $KEY_MASAK INTEGER,
            $KEY_DIBAYAR INTEGER,
            $KEY_KIRIM INTEGER,
            $KEY_LAT VARCHAR,
            $KEY_LON VARCHAR,
            $KEY_PHOTO VARCHAR,
            $DB_ARCHIVE INTEGER
        )
        """.trimIndent()

    override fun onCreate(db: SQLiteDatabase?) {
//        db?.execSQL(createTablePanenTBS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
//        if (oldVersion < 2) {
//            db.execSQL(createTableSixHours)
//        }
    }

}