package com.cbi.mobile_plantation.utils

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

    var registeredDeviceUsername: String?
        get() = pref.getString(REGISTERED_DEVICE_USERNAME, "")
        set(value) {
            editor.putString(REGISTERED_DEVICE_USERNAME, value)
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

    // Fix 1: Update your PrefManager to handle mixed types
    var afdelingIdUserLogin: String?
        get() {
            return try {
                // Try to get as String first
                val value = pref.getString("afdelingIdUserLogin", "")
                if (value != null) return value

                // If that fails, try to get as Int and convert to String
                val intValue = pref.getInt("afdelingIdUserLogin", 0)
                return intValue.toString()
            } catch (e: ClassCastException) {
                // Handle the case where it's stored as an Int
                try {
                    val intValue = pref.getInt("afdelingIdUserLogin", 0)
                    // Store it back as String for future use
                    editor.putString("afdelingIdUserLogin", intValue.toString())
                    editor.apply()
                    return intValue.toString()
                } catch (e2: Exception) {
                    AppLogger.e("Error getting afdelingIdUserLogin: ${e2.message}")
                    return ""
                }
            } catch (e: Exception) {
                AppLogger.e("Error getting afdelingIdUserLogin: ${e.message}")
                return ""
            }
        }
        set(afdelingIdUserLogin) {
            editor.putString("afdelingIdUserLogin", afdelingIdUserLogin)
            editor.apply() // Use apply instead of commit for better performance
        }

    var lastModifiedDatasetEstate: String?
        get() = pref.getString("lastModifiedDatasetEstate", "")
        set(lastModifiedDatasetEstate) {
            editor.putString("lastModifiedDatasetEstate", lastModifiedDatasetEstate)
            editor.commit()
        }

    private val PREF_ESTATE_LAST_MODIFIED_PREFIX = "estate_last_modified_"

    // Set last modified timestamp for a specific estate
    fun setEstateLastModified(estateAbbr: String, timestamp: String) {
        val editor = pref.edit()
        editor.putString("$PREF_ESTATE_LAST_MODIFIED_PREFIX$estateAbbr", timestamp)
        editor.apply()
    }


    fun getMasterTPHEstateLastModifiedMap(): Map<String, String> {
        val allEntries = pref.all
        val estateMap = mutableMapOf<String, String>()

        for ((key, value) in allEntries) {
            if (key.startsWith(PREF_ESTATE_LAST_MODIFIED_PREFIX) && value is String) {
                val estateAbbr = key.removePrefix(PREF_ESTATE_LAST_MODIFIED_PREFIX)
                estateMap[estateAbbr] = value
            }
        }

        return estateMap
    }

    // Get last modified timestamp for a specific estate
    fun getEstateLastModified(estateAbbr: String): String? {
        return pref.getString("$PREF_ESTATE_LAST_MODIFIED_PREFIX$estateAbbr", null)
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

    var kemandoranUserLogin: String?
        get() = pref.getString("kemandoranUserLogin", "")
        set(kemandoranUserLogin) {
            editor.putString("kemandoranUserLogin", kemandoranUserLogin)
            editor.commit()
        }

    var kemandoranNamaUserLogin: String?
        get() = pref.getString("kemandoranNamaUserLogin", "")
        set(kemandoranNamaUserLogin) {
            editor.putString("kemandoranNamaUserLogin", kemandoranNamaUserLogin)
            editor.commit()
        }

    var kemandoranKodeUserLogin: String?
        get() = pref.getString("kemandoranKodeUserLogin", "")
        set(kemandoranKodeUserLogin) {
            editor.putString("kemandoranKodeUserLogin", kemandoranKodeUserLogin)
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

    var lastModifiedDatasetJenisTPH: String?
        get() = pref.getString("lastModifiedDatasetJenisTPH", "")
        set(lastModifiedDatasetJenisTPH) {
            editor.putString("lastModifiedDatasetJenisTPH", lastModifiedDatasetJenisTPH)
            editor.commit()
        }

    var lastSyncDate: String?
        get() = pref.getString("lastSyncDate", "")
        set(lastSyncDate) {
            editor.putString("lastSyncDate", lastSyncDate)
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

    var lastModifiedDatasetKendaraan: String?
        get() = pref.getString("lastModifiedDatasetKendaraan", "")
        set(lastModifiedDatasetKendaraan) {
            editor.putString("lastModifiedDatasetKendaraan", lastModifiedDatasetKendaraan)
            editor.commit()
        }

    var lastModifiedSettingJSON: String?
        get() = pref.getString("lastModifiedSettingJSON", "")
        set(lastModifiedSettingJSON) {
            editor.putString("lastModifiedSettingJSON", lastModifiedSettingJSON)
            editor.commit()
        }

    var radiusMinimum: Float
        get() = pref.getFloat("radiusMinimum", 80F) // Default 80F
        set(value) {
            editor.putFloat("radiusMinimum", value)
            editor.commit()
        }

    var boundaryAccuracy: Float
        get() = pref.getFloat("boundaryAccuracy", 15F) // Default 15F
        set(value) {
            editor.putFloat("boundaryAccuracy", value)
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

        private const val REGISTERED_DEVICE_USERNAME = "registered_device_username"
    }

    init {
        pref = _context.getSharedPreferences(PREF_NAME, privateMode)
        editor = pref.edit()
    }
}