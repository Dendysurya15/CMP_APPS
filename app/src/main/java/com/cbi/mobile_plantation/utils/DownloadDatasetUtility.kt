package com.cbi.mobile_plantation.utils

import com.cbi.mobile_plantation.data.model.User
import com.cbi.mobile_plantation.data.model.dataset.DatasetRequest
import com.cbi.mobile_plantation.ui.viewModel.DatasetViewModel

class DownloadDatasetUtility(
    private val prefManager: PrefManager,
    private val datasetViewModel: DatasetViewModel
) {

    companion object {
        private const val TAG = "DownloadDatasetUtility"
    }

    // Flags for triggers (you'll need to pass these or access them somehow)
    var isTriggerFeatureInspection = false
    var isTriggerFollowUp = false
    var isTriggerButtonSinkronisasiData = false

    fun getDatasetsToDownload(
        regionalId: Int,
        estateId: Int,
        afdelingId: String,
        lastModifiedDatasetEstate: String?,
        lastModifiedDatasetTPH: String?,
        lastModifiedDatasetJenisTPH: String?,
        lastModifiedDatasetBlok: String?,
        lastModifiedDatasetPemanen: String?,
        lastModifiedDatasetKemandoran: String?,
        lastModifiedDatasetTransporter: String?,
        lastModifiedDatasetKendaraan: String?,
        lastModifiedSettingJSON: String?
    ): List<DatasetRequest> {
        val datasets = mutableListOf<DatasetRequest>()

        val jabatan = prefManager.jabatanUserLogin
        val regionalUser = prefManager.regionalIdUserLogin!!.toInt()

        // Define user roles
        val userRole = getUserRole(jabatan!!)

        AppLogger.d("$TAG - User role: $userRole")

        // Handle special triggers first (these override normal role-based logic)
        if (handleSpecialTriggers(datasets, userRole, estateId, afdelingId)) {
            return datasets
        }

        // Add role-specific datasets
        addRoleSpecificDatasets(
            datasets, userRole, regionalId, estateId, afdelingId, regionalUser,
            lastModifiedDatasetEstate, lastModifiedDatasetTPH, lastModifiedDatasetBlok,
            lastModifiedDatasetPemanen
        )

        // Add common datasets for all roles (except when special triggers are active)
        addCommonDatasets(
            datasets, regionalId, estateId,afdelingId, lastModifiedDatasetJenisTPH,
            lastModifiedDatasetKemandoran, lastModifiedDatasetTransporter,
            lastModifiedDatasetKendaraan, lastModifiedSettingJSON
        )

        return datasets
    }

    private enum class UserRole {
        KERANI_TIMBANG,
        KERANI_PANEN,
        MANDOR_1,
        MANDOR_PANEN,
        ASISTEN,
        OTHER
    }

    private fun getUserRole(jabatan: String): UserRole {
        return when {
            jabatan.contains(
                AppUtils.ListFeatureByRoleUser.KeraniTimbang,
                ignoreCase = true
            ) -> UserRole.KERANI_TIMBANG

            jabatan.contains(
                AppUtils.ListFeatureByRoleUser.KeraniPanen,
                ignoreCase = true
            ) -> UserRole.KERANI_PANEN

            jabatan.contains(
                AppUtils.ListFeatureByRoleUser.Mandor1,
                ignoreCase = true
            ) -> UserRole.MANDOR_1

            jabatan.contains(
                AppUtils.ListFeatureByRoleUser.MandorPanen,
                ignoreCase = true
            ) -> UserRole.MANDOR_PANEN

            jabatan.contains(
                AppUtils.ListFeatureByRoleUser.Asisten,
                ignoreCase = true
            ) -> UserRole.ASISTEN

            else -> UserRole.OTHER
        }
    }

    private fun handleSpecialTriggers(
        datasets: MutableList<DatasetRequest>,
        userRole: UserRole,
        estateId: Int,
        afdelingId: String
    ): Boolean {

        if (isTriggerButtonSinkronisasiData && userRole != UserRole.KERANI_PANEN && userRole != UserRole.KERANI_TIMBANG) {
            datasets.add(
                DatasetRequest(
                    afdeling = afdelingId,
                    estate = estateId,
                    lastModified = null,
                    dataset = AppUtils.DatasetNames.sinkronisasiDataPanen
                )
            )
        }

        if (!isTriggerButtonSinkronisasiData && userRole != UserRole.KERANI_PANEN && userRole != UserRole.KERANI_TIMBANG) {
            datasets.add(
                DatasetRequest(
                    afdeling = afdelingId,
                    estate = estateId,
                    lastModified = null,
                    dataset = AppUtils.DatasetNames.sinkronisasiDataPanen
                )
            )
        }

        if (isTriggerButtonSinkronisasiData && (userRole == UserRole.MANDOR_1 || userRole == UserRole.ASISTEN )) {
            datasets.add(
                DatasetRequest(
                    afdeling = afdelingId,
                    estate = estateId,
                    lastModified = null,
                    dataset = AppUtils.DatasetNames.sinkronisasiRestan
                )
            )
        }

        if (isTriggerButtonSinkronisasiData && userRole != UserRole.KERANI_PANEN) {
            datasets.add(
                DatasetRequest(
                    regional = null,
                    lastModified = null,
                    dataset = AppUtils.DatasetNames.parameter
                )
            )
        }


        // Handle follow-up trigger
        if (isTriggerFollowUp && userRole != UserRole.KERANI_PANEN) {
            datasets.add(
                DatasetRequest(
                    afdeling = afdelingId,
                    estate = estateId,
                    lastModified = null,
                    dataset = AppUtils.DatasetNames.sinkronisasiFollowUpInspeksi
                )
            )
            return true // Early return - stop processing
        }

        // Handle sync data button trigger
        if (isTriggerButtonSinkronisasiData && userRole != UserRole.KERANI_TIMBANG) {
            handleSyncDataButtonTrigger(datasets)
            // Don't return true here - we still want to add other datasets
        }


        return false // Continue with normal processing
    }

    private fun handleSyncDataButtonTrigger(datasets: MutableList<DatasetRequest>) {
        val estateTimestamps = prefManager.getMasterTPHEstateLastModifiedMap()
        AppLogger.d("$TAG - Estate timestamps (${estateTimestamps.size} estates):")

        if (estateTimestamps.isNotEmpty()) {
            estateTimestamps.forEach { (abbr, timestamp) ->
                AppLogger.d("$TAG - $abbr: $timestamp")

                val estate = datasetViewModel.allEstatesList.value?.find { it.abbr == abbr }
                val estateId = estate?.id?.toInt()
                val estateName = estate?.nama ?: "Unknown Estate"

                if (estateId != null) {
                    AppLogger.d("$TAG - Adding estate dataset: $abbr ($estateName)")
                    datasets.add(
                        DatasetRequest(
                            estate = estateId,
                            estateAbbr = abbr,
                            lastModified = timestamp,
                            dataset = AppUtils.DatasetNames.tph
                        )
                    )
                } else {
                    AppLogger.d("$TAG - Skipping estate $abbr - could not find estate ID")
                }
            }
        } else {
            AppLogger.d("$TAG - No estate timestamps found to process")
        }

        // Add user sync dataset
        datasets.add(
            DatasetRequest(
                lastModified = null,
                idUser = prefManager.idUserLogin,
                dataset = AppUtils.DatasetNames.sinkronisasiDataUser
            )
        )
    }

    private fun addRoleSpecificDatasets(
        datasets: MutableList<DatasetRequest>,
        userRole: UserRole,
        regionalId: Int,
        estateId: Int,
        afdelingId: String,
        regionalUser: Int,
        lastModifiedDatasetEstate: String?,
        lastModifiedDatasetTPH: String?,
        lastModifiedDatasetBlok: String?,
        lastModifiedDatasetPemanen: String?
    ) {
        // Add parameter dataset (common for most cases)

        when (userRole) {
            UserRole.KERANI_TIMBANG -> {
                addKeraniTimbangDatasets(
                    datasets, regionalUser, estateId, regionalId,
                    lastModifiedDatasetBlok, lastModifiedDatasetTPH, lastModifiedDatasetPemanen
                )
            }

            UserRole.MANDOR_PANEN, UserRole.MANDOR_1, UserRole.ASISTEN -> {
                addMandorDatasets(
                    datasets,
                    regionalUser,
                    estateId,
                    lastModifiedDatasetBlok,
                    lastModifiedDatasetTPH,
                    lastModifiedDatasetPemanen,
                    lastModifiedDatasetEstate
                )
            }

            UserRole.KERANI_PANEN, UserRole.OTHER -> {
                addDefaultUserDatasets(
                    datasets, estateId, regionalUser,
                    lastModifiedDatasetTPH, lastModifiedDatasetPemanen, lastModifiedDatasetEstate
                )
            }
        }
    }

    private fun addKeraniTimbangDatasets(
        datasets: MutableList<DatasetRequest>,
        regionalUser: Int,
        estateId: Int,
        regionalId: Int,
        lastModifiedDatasetBlok: String?,
        lastModifiedDatasetTPH: String?,
        lastModifiedDatasetPemanen: String?
    ) {
        datasets.addAll(
            listOf(
                DatasetRequest(
                    regional = regionalUser,
                    lastModified = lastModifiedDatasetBlok,
                    dataset = AppUtils.DatasetNames.blok
                ),
                DatasetRequest(
                    estate = estateId,
                    lastModified = lastModifiedDatasetTPH,
                    dataset = AppUtils.DatasetNames.tph
                ),
                DatasetRequest(
                    regional = regionalId,
                    lastModified = lastModifiedDatasetPemanen,
                    dataset = AppUtils.DatasetNames.pemanen
                )
            )
        )
    }

    private fun addMandorDatasets(
        datasets: MutableList<DatasetRequest>,
        regionalUser: Int,
        estateId: Int,
        lastModifiedDatasetBlok: String?,
        lastModifiedDatasetTPH: String?,
        lastModifiedDatasetPemanen: String?,
        lastModifiedDatasetEstate: String?
    ) {
        datasets.addAll(
            listOf(
                DatasetRequest(
                    regional = regionalUser,
                    lastModified = lastModifiedDatasetBlok,
                    dataset = AppUtils.DatasetNames.blok
                ),
                DatasetRequest(
                    estate = estateId,
                    lastModified = lastModifiedDatasetTPH,
                    dataset = AppUtils.DatasetNames.tph
                ),
                DatasetRequest(
                    estate = estateId,
                    lastModified = lastModifiedDatasetPemanen,
                    dataset = AppUtils.DatasetNames.pemanen
                ),
                DatasetRequest(
                    regional = regionalUser,
                    lastModified = lastModifiedDatasetEstate,
                    dataset = AppUtils.DatasetNames.estate
                )
            )
        )
    }

    private fun addDefaultUserDatasets(
        datasets: MutableList<DatasetRequest>,
        estateId: Int,
        regionalUser: Int,
        lastModifiedDatasetTPH: String?,
        lastModifiedDatasetPemanen: String?,
        lastModifiedDatasetEstate: String?
    ) {
        datasets.addAll(
            listOf(
                DatasetRequest(
                    estate = estateId,
                    lastModified = lastModifiedDatasetTPH,
                    dataset = AppUtils.DatasetNames.tph
                ),
                DatasetRequest(
                    estate = estateId,
                    lastModified = lastModifiedDatasetPemanen,
                    dataset = AppUtils.DatasetNames.pemanen
                ),
                DatasetRequest(
                    regional = regionalUser,
                    lastModified = lastModifiedDatasetEstate,
                    dataset = AppUtils.DatasetNames.estate
                )
            )
        )
    }

    private fun addCommonDatasets(
        datasets: MutableList<DatasetRequest>,
        regionalId: Int,
        estateId: Int,
        afdelingId:String,
        lastModifiedDatasetJenisTPH: String?,
        lastModifiedDatasetKemandoran: String?,
        lastModifiedDatasetTransporter: String?,
        lastModifiedDatasetKendaraan: String?,
        lastModifiedSettingJSON: String?
    ) {
        datasets.addAll(
            listOf(
                DatasetRequest(
                    regional = regionalId,
                    lastModified = null,
                    dataset = AppUtils.DatasetNames.mill
                ),

                DatasetRequest(
                    lastModified = lastModifiedDatasetJenisTPH,
                    dataset = AppUtils.DatasetNames.jenisTPH
                ),
                DatasetRequest(
                    estate = estateId,
                    lastModified = lastModifiedDatasetKemandoran,
                    dataset = AppUtils.DatasetNames.kemandoran
                ),
                DatasetRequest(
                    lastModified = lastModifiedDatasetTransporter,
                    dataset = AppUtils.DatasetNames.transporter
                ),
                DatasetRequest(
                    lastModified = lastModifiedDatasetKendaraan,
                    dataset = AppUtils.DatasetNames.kendaraan
                ),
                DatasetRequest(
                    lastModified = lastModifiedSettingJSON,
                    dataset = AppUtils.DatasetNames.settingJSON
                )
            )
        )
    }
}