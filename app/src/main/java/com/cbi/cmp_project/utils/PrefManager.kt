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

    fun setDateModified(key: String, value: String?) {
        editor.putString(key, value)
        editor.commit()
    }

    fun getAllDateModified(): Map<String, String?> {
        val allEntries = pref.all
        val dateModifiedMap = mutableMapOf<String, String?>()

        for ((key, value) in allEntries) {
            // Check if the key matches your convention for date_modified keys
            if (key.endsWith("DB") && value is String) {
                dateModifiedMap[key] = value
            }
        }

        return dateModifiedMap
    }

    // Clear the file list
    fun clearFileList() {
        editor.remove("downloaded_file_list").apply()
    }

    var isFirstTimeLaunch: Boolean
        get() = pref.getBoolean("IsFirstTimeLaunch", true)  // Default is true
        set(isFirstTime) {
            editor.putBoolean("IsFirstTimeLaunch", isFirstTime)
            editor.apply()  // Use apply() instead of commit() for async write
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
    }

    init {
        pref = _context.getSharedPreferences(PREF_NAME, privateMode)
        editor = pref.edit()
    }
}