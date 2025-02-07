package com.cbi.cmp_project.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PrefManager(_context: Context) {
    private var pref: SharedPreferences
    private var editor: SharedPreferences.Editor
    var privateMode = 0

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

    var password: String?
        get() = pref.getString(PASSWORD, "")
        set(password) {
            editor.putString(PASSWORD, password)
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

    var version: Int
        get() = pref.getInt(version_tag, 0)
        set(versionCount) {
            editor.putInt(version_tag, versionCount)
            editor.commit()
        }




    companion object {
        // Shared preferences file name
        private const val PREF_NAME = "cbi_cmp"

        const val version_tag = "version"

        const val REMEMBERME = "remember_me"
        const val USERNAME = "username"
        const val PASSWORD = "password"
    }

    init {
        pref = _context.getSharedPreferences(PREF_NAME, privateMode)
        editor = pref.edit()
    }
}