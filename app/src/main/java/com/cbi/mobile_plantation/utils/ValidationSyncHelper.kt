package com.cbi.mobile_plantation.utils

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.AbsensiKemandoranRelations
import com.cbi.mobile_plantation.data.model.AfdelingModel
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.ui.viewModel.AbsensiViewModel
import com.cbi.mobile_plantation.ui.viewModel.DatasetViewModel
import com.cbi.mobile_plantation.ui.viewModel.PanenViewModel
import kotlinx.coroutines.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object ValidationSyncHelper {

    /**
     * Validates afdeling for users who don't skip afdeling check
     */
    suspend fun validateAfdeling(
        context: Context,
        prefManager: PrefManager,
        datasetViewModel: DatasetViewModel,
        shouldSkipAfdelingCheck: Boolean
    ): AfdelingModel? {
        if (shouldSkipAfdelingCheck) return null

        val afdelingId = prefManager.afdelingIdUserLogin

        if (afdelingId?.lowercase() == "x" || afdelingId.isNullOrEmpty()) {
            showAfdelingInvalidDialog(context, afdelingId)
            return null
        }

        val afdeling = withContext(Dispatchers.IO) {
            datasetViewModel.getAfdelingById(afdelingId.toInt())
        }

        if (afdeling == null) {
            showAfdelingNotFoundDialog(context)
            return null
        }

        return afdeling
    }

    /**
     * Validates sync date for given sync preference key
     */
    suspend fun validateSyncDate(
        context: Context,
        lastSyncDateTime: String?,
        checkCurrentDate: Boolean = true
    ): Boolean {
        if (lastSyncDateTime.isNullOrEmpty()) {
            showSyncRequiredDialog(context, "Database belum pernah disinkronisasi. Silakan lakukan sinkronisasi terlebih dahulu sebelum menggunakan fitur ini.")
            return false
        }

        if (!checkCurrentDate) return true

        return try {
            val lastSyncDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(lastSyncDateTime)!!
            )
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            if (lastSyncDate != currentDate) {
                showSyncRequiredDialog(context, "Database perlu disinkronisasi untuk hari ini. Silakan lakukan sinkronisasi terlebih dahulu sebelum menggunakan fitur ini.")
                false
            } else {
                true
            }
        } catch (e: ParseException) {
            AppLogger.e("Error parsing sync date: ${e.message}")
            showSyncRequiredDialog(context, "Data sinkronisasi tidak valid. Silakan lakukan sinkronisasi ulang terlebih dahulu sebelum menggunakan fitur ini.")
            false
        }
    }

    /**
     * Checks if user should skip afdeling validation based on jabatan
     */
    fun shouldSkipAfdelingCheck(jabatanUser: String?): Boolean {
        return jabatanUser?.lowercase()?.let { jabatan ->
                    jabatan.contains(AppUtils.ListFeatureByRoleUser.GM)
        } ?: false
    }

    /**
     * Validates present karyawan for PanenTBS feature
     */
    suspend fun validatePresentKaryawan(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        absensiViewModel: AbsensiViewModel,
        panenViewModel: PanenViewModel
    ): List<KaryawanModel>? {
        // Load active absensi
        val absensiDeferred = CompletableDeferred<List<AbsensiKemandoranRelations>>()

        absensiViewModel.loadActiveAbsensi()
        delay(100)

        withContext(Dispatchers.Main) {
            absensiViewModel.activeAbsensiList.observe(lifecycleOwner) { absensiWithRelations ->
                val absensiData = absensiWithRelations ?: emptyList()
                absensiDeferred.complete(absensiData)
            }
        }

        val absensiData = absensiDeferred.await()

        // Extract present NIKs
        val presentNikSet = mutableSetOf<String>()
        absensiData.forEach { absensiRelation ->
            val absensi = absensiRelation.absensi
            val niks = absensi.karyawan_msk_nik.split(",")
            presentNikSet.addAll(niks.filter { it.isNotEmpty() && it.trim().isNotEmpty() })
        }

        // Get present karyawan
        val karyawanDeferred = CompletableDeferred<List<KaryawanModel>>()

        panenViewModel.getAllKaryawan()
        delay(100)

        withContext(Dispatchers.Main) {
            panenViewModel.allKaryawanList.observe(lifecycleOwner) { list ->
                val allKaryawan = list ?: emptyList()
                val presentKaryawan = if (presentNikSet.isNotEmpty()) {
                    allKaryawan.filter { karyawan ->
                        karyawan.nik != null && presentNikSet.contains(karyawan.nik)
                    }
                } else {
                    emptyList()
                }

                AppLogger.d("Total karyawan: ${allKaryawan.size}")
                AppLogger.d("Filtered to present karyawan: ${presentKaryawan.size}")

                karyawanDeferred.complete(presentKaryawan)
            }
        }

        val presentKaryawan = karyawanDeferred.await()

        if (presentKaryawan.isEmpty()) {
            showNoKaryawanDialog(context)
            return null
        }

        return presentKaryawan
    }

    /**
     * Validates sync requirement based on afdeling's sinkronisasi_otomatis setting
     */
    suspend fun validateSyncRequirement(
        context: Context,
        afdeling: AfdelingModel?,
        prefManager: PrefManager?
    ): Boolean {
        if (afdeling == null || afdeling.sinkronisasi_otomatis == 0) {
            return true // Skip sync validation
        }

        val lastSyncDateTime = prefManager?.lastSyncDate
        return validateSyncDate(context, lastSyncDateTime, true)
    }

    // Dialog helper functions
    private fun showAfdelingInvalidDialog(context: Context, afdelingId: String?) {
        AlertDialogUtility.withSingleAction(
            context,
            "Kembali",
            "Afdeling tidak valid",
            "Data afdeling saat ini adalah $afdelingId",
            "warning.json",
            R.color.colorRedDark
        ) {}
    }

    private fun showAfdelingNotFoundDialog(context: Context) {
        AlertDialogUtility.withSingleAction(
            context,
            "Kembali",
            "Data Afdeling Tidak Ditemukan",
            "Data afdeling tidak ditemukan di database lokal. Silakan sinkronisasi terlebih dahulu.",
            "warning.json",
            R.color.colorRedDark
        ) {}
    }

    private fun showSyncRequiredDialog(context: Context, message: String) {
        AlertDialogUtility.withSingleAction(
            context,
            "Kembali",
            "Sinkronisasi Database Diperlukan",
            message,
            "warning.json",
            R.color.colorRedDark
        ) {}
    }

    private fun showNoKaryawanDialog(context: Context) {
        AlertDialogUtility.withSingleAction(
            context,
            "Kembali",
            "Data Karyawan Tidak Hadir",
            "Tidak ditemukan data kehadiran karyawan untuk hari ini.\nSilakan melakukan scan QR Absensi dari Mandor Panen.",
            "warning.json",
            R.color.colorRedDark
        ) {}
    }

    fun showErrorDialog(context: Context) {
        AlertDialogUtility.withSingleAction(
            context,
            "Kembali",
            "Terjadi Kesalahan",
            "Gagal melakukan validasi data. Silakan coba lagi.",
            "error.json",
            R.color.colorRedDark
        ) {}
    }
}