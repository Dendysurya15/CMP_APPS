package com.cbi.cmp_project.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PrefManager(_context: Context) {
    private var pref: SharedPreferences
    private var editor: SharedPreferences.Editor
    var privateMode = 0

    fun saveFileList(fileList: List<String?>) {
        val json = Gson().toJson(fileList)
        editor.putString("downloaded_file_list", json)
        editor.apply()
    }

    // Retrieve the list of files
    fun getFileList(): List<String?> {
        val json = pref.getString("downloaded_file_list", "[]")
        return try {
            val type = object : TypeToken<List<String?>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    var rememberLogin: Boolean
        get() = pref.getBoolean(REMEMBERME, false)
        set(rememberLogin) {
            editor.putBoolean(REMEMBERME, rememberLogin)
            editor.commit()
        }

    var username: String?
        get() = pref.getString(USERNAME, "")
        set(username) {
            editor.putString(USERNAME, username)
            editor.commit()
        }

    var nameUserLogin: String?
        get() = pref.getString("nameUserLogin", "")
        set(nameUserLogin) {
            editor.putString("nameUserLogin", nameUserLogin)
            editor.commit()
        }

    var idUserLogin: Int?
        get() = pref.getInt("idUserLogin", 0)
        set(idUserLogin) {
            editor.putInt("idUserLogin", idUserLogin!!)
            editor.commit()
        }

    var jabatanUserLogin: String?
        get() = pref.getString("jabatanUserLogin", "")
        set(jabatanUserLogin) {
            editor.putString("jabatanUserLogin", jabatanUserLogin)
            editor.commit()
        }

    var estateUserLogin: String?
        get() = pref.getString("estateUserLogin", "")
        set(estateUserLogin) {
            editor.putString("estateUserLogin", estateUserLogin)
            editor.commit()
        }

    var estateUserLengkapLogin: String?
        get() = pref.getString("estateUserLengkapLogin", "")
        set(estateUserLengkapLogin) {
            editor.putString("estateUserLengkapLogin", estateUserLengkapLogin)
            editor.commit()
        }

    var estateIdUserLogin: String?
        get() = pref.getString("estateIdUserLogin", "")
        set(estateIdUserLogin) {
            editor.putString("estateIdUserLogin", estateIdUserLogin)
            editor.commit()
        }

    var regionalIdUserLogin: String?
        get() = pref.getString("regionalIdUserLogin", "")
        set(regionalIdUserLogin) {
            editor.putString("regionalIdUserLogin", regionalIdUserLogin)
            editor.commit()
        }

    var companyIdUserLogin: String?
        get() = pref.getString("companyIdUserLogin", "")
        set(companyIdUserLogin) {
            editor.putString("companyIdUserLogin", companyIdUserLogin)
            editor.commit()
        }

    var companyAbbrUserLogin: String?
        get() = pref.getString("companyAbbrUserLogin", "")
        set(companyAbbrUserLogin) {
            editor.putString("companyAbbrUserLogin", companyAbbrUserLogin)
            editor.commit()
        }

    var companyNamaUserLogin: String?
        get() = pref.getString("companyNamaUserLogin", "")
        set(companyNamaUserLogin) {
            editor.putString("companyNamaUserLogin", companyNamaUserLogin)
            editor.commit()
        }

    var password: String?
        get() = pref.getString(PASSWORD, "")
        set(password) {
            editor.putString(PASSWORD, password)
            editor.commit()
        }

    var lastModifiedDatasetTPH: String?
        get() = pref.getString("lastModifiedDatasetTPH", "")
        set(lastModifiedDatasetTPH) {
            editor.putString("lastModifiedDatasetTPH", lastModifiedDatasetTPH)
            editor.commit()
        }

    var lastModifiedDatasetBlok: String?
        get() = pref.getString("lastModifiedDatasetBlok", "")
        set(lastModifiedDatasetBlok) {
            editor.putString("lastModifiedDatasetBlok", lastModifiedDatasetBlok)
            editor.commit()
        }

    var lastModifiedDatasetKemandoran: String?
        get() = pref.getString("lastModifiedDatasetKemandoran", "")
        set(lastModifiedDatasetKemandoran) {
            editor.putString("lastModifiedDatasetKemandoran", lastModifiedDatasetKemandoran)
            editor.commit()
        }

    var lastModifiedDatasetPemanen: String?
        get() = pref.getString("lastModifiedDatasetPemanen", "")
        set(lastModifiedDatasetPemanen) {
            editor.putString("lastModifiedDatasetPemanen", lastModifiedDatasetPemanen)
            editor.commit()
        }

    var lastModifiedDatasetTransporter: String?
        get() = pref.getString("lastModifiedDatasetTransporter", "")
        set(lastModifiedDatasetTransporter) {
            editor.putString("lastModifiedDatasetTransporter", lastModifiedDatasetTransporter)
            editor.commit()
        }

    var token: String?
        get() = pref.getString("token", "")
        set(token) {
            editor.putString("token", token)
            editor.commit()
        }



    var isFirstTimeLaunch: Boolean
        get() = pref.getBoolean("IsFirstTimeLaunch", false)  // Default is true
        set(isFirstTime) {
            editor.putBoolean("IsFirstTimeLaunch", isFirstTime)
            editor.apply()  // Use apply() instead of commit() for async write
        }

    var datasetMustUpdate: MutableList<String>
        get() = pref.getStringSet("datasetMustUpdate", emptySet())?.toMutableList() ?: mutableListOf()
        set(value) {
            editor.putStringSet("datasetMustUpdate", value.toSet()).apply()
        }

    fun addDataset(dataset: String) {
        val currentList = datasetMustUpdate.toMutableSet()
        currentList.add(dataset)
        datasetMustUpdate = currentList.toMutableList()
    }

    fun clearDatasetMustUpdate() {
        datasetMustUpdate = mutableListOf()
    }

    var version: Int
        get() = pref.getInt(version_tag, 0)
        set(versionCount) {
            editor.putInt(version_tag, versionCount)
            editor.commit()
        }

    fun setRegionalUserLogin(key: String, value: String?) {
        editor.putString(key, value)
        editor.commit() // Immediately save changes
    }

    // Save estate user login
    fun setEstateUserLogin(key: String, value: String?) {
        editor.putString(key, value)
        editor.commit()
    }

    fun setUserNameLogin(key: String, value: String?) {
        editor.putString(key, value)
        editor.commit()
    }

    fun setJabatanUserLogin(key: String, value: String?) {
        editor.putString(key, value)
        editor.commit()
    }

    fun setUserIdLogin(key: String, value: String?) {
        editor.putString(key, value)
        editor.commit()
    }

    fun getRegionalUserLogin(key: String): String? {
        return pref.getString(key, null)
    }

    // Get estate user login
    fun getEstateUserLogin(key: String): String? {
        return pref.getString(key, null)
    }

    fun getUserNameLogin(key: String): String? {
        return pref.getString(key, null)
    }

    fun getUserIdLogin(key: String): String? {
        return pref.getString(key, null)
    }

    fun getJabatanUserLogin(key: String): String? {
        return pref.getString(key, null)
    }


    var user_input: String?
        get() = pref.getString("user_input", "")
        set(hexDataWl) {
            editor.putString("user_input", hexDataWl)
            editor.commit()
        }

    var id_selected_estate: Int?
        get() = pref.getInt("id_selected_estate", 0)
        set(hexDataWl) {
            editor.putInt("id_selected_estate", hexDataWl!!)
            editor.commit()
        }

    var id_selected_afdeling: Int?
        get() = pref.getInt("id_selected_afdeling", 0)
        set(hexDataWl) {
            editor.putInt("id_selected_afdeling", hexDataWl!!)
            editor.commit()
        }

    var id_selected_blok: Int?
        get() = pref.getInt("id_selected_blok", 0)
        set(hexDataWl) {
            editor.putInt("id_selected_blok", hexDataWl!!)
            editor.commit()
        }

    var id_selected_ancak: Int?
        get() = pref.getInt("id_selected_ancak", 0)
        set(hexDataWl) {
            editor.putInt("id_selected_ancak", hexDataWl!!)
            editor.commit()
        }

    var id_selected_tph: Int?
        get() = pref.getInt("id_selected_tph", 0)
        set(hexDataWl) {
            editor.putInt("id_selected_tph", hexDataWl!!)
            editor.commit()
        }




    companion object {
        // Shared preferences file name
        private const val PREF_NAME = "marker_tph"
        private const val IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch"
        private const val LOGIN = "Login"
        private const val SESSION = "Session"

        const val version_tag = "version"

        const val user_input = "user_input"
        const val REMEMBERME = "remember_me"
        const val USERNAME = "username"
        const val PASSWORD = "password"
    }

    init {
        pref = _context.getSharedPreferences(PREF_NAME, privateMode)
        editor = pref.edit()
    }
}