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
        estateId: Any,
        afdelingId: Any,
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

        // Add role-specific datasets
        addRoleSpecificDatasets(
            datasets, userRole, regionalId, estateId, afdelingId, regionalUser,lastModifiedDatasetKemandoran,
            lastModifiedDatasetEstate, lastModifiedDatasetTPH, lastModifiedDatasetBlok,
            lastModifiedDatasetPemanen
        )


//        // Handle special triggers first (these override normal role-based logic)
        if (handleSpecialTriggers(datasets, userRole, estateId, afdelingId)) {
            return datasets
        }


        addCommonDatasets(
            datasets,userRole, regionalId, estateId,afdelingId, lastModifiedDatasetJenisTPH,
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
        ASKEP,
        MANAGER,
        GM,
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

            jabatan.contains(
                AppUtils.ListFeatureByRoleUser.ASKEP,
                ignoreCase = true
            ) -> UserRole.ASKEP

            jabatan.contains(
                AppUtils.ListFeatureByRoleUser.Manager,
                ignoreCase = true
            ) -> UserRole.MANAGER

            jabatan.contains(
                AppUtils.ListFeatureByRoleUser.GM,
                ignoreCase = true
            ) -> UserRole.GM

            else -> UserRole.OTHER
        }
    }

    private fun handleSpecialTriggers(
        datasets: MutableList<DatasetRequest>,
        userRole: UserRole,
        estateId: Any,
        afdelingId: Any
    ): Boolean {

        // Only add other datasets if NOT follow-up trigger
        if (isTriggerButtonSinkronisasiData && userRole != UserRole.KERANI_PANEN && userRole != UserRole.KERANI_TIMBANG) {
            datasets.add(
                DatasetRequest(
                    afdeling = afdelingId,
                    estate = estateId,
                    lastModified = null,
                    dataset = AppUtils.DatasetNames.sinkronisasiDataPanen
                )
            )
            datasets.add(
                DatasetRequest(
                    afdeling = afdelingId,
                    estate = estateId,
                    lastModified = null,
                    dataset = AppUtils.DatasetNames.sinkronisasiFollowUpInspeksi
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
            datasets.add(
                DatasetRequest(
                    afdeling = afdelingId,
                    estate = estateId,
                    lastModified = null,
                    dataset = AppUtils.DatasetNames.sinkronisasiFollowUpInspeksi
                )
            )
            datasets.add(
                DatasetRequest(
                    regional = null,
                    lastModified = null,
                    dataset = AppUtils.DatasetNames.parameter
                )
            )
        }

        if (isTriggerButtonSinkronisasiData && (userRole == UserRole.MANDOR_1 || userRole == UserRole.ASISTEN)) {
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
        estateId: Any,
        afdelingId: Any,
        regionalUser: Int,
        lastModifiedDatasetKemandoran:String?,
        lastModifiedDatasetEstate: String?,
        lastModifiedDatasetTPH: String?,
        lastModifiedDatasetBlok: String?,
        lastModifiedDatasetPemanen: String?
    ) {
        AppLogger.d("alskjdlkajsd flkjsflk j")
        when (userRole) {
            UserRole.KERANI_TIMBANG -> {
                addKeraniTimbangDatasets(
                    datasets, regionalUser, estateId, regionalId,lastModifiedDatasetEstate,lastModifiedDatasetKemandoran,
                    lastModifiedDatasetBlok, lastModifiedDatasetTPH, lastModifiedDatasetPemanen
                )
            }

            UserRole.MANDOR_PANEN, UserRole.MANDOR_1, UserRole.ASISTEN -> {
                addMandorDatasets(
                    datasets,
                    regionalUser,
                    estateId,
                    lastModifiedDatasetKemandoran,
                    lastModifiedDatasetBlok,
                    lastModifiedDatasetTPH,
                    lastModifiedDatasetPemanen,
                    lastModifiedDatasetEstate
                )
            }

            UserRole.KERANI_PANEN, UserRole.MANAGER, UserRole.ASKEP, UserRole.OTHER -> {
                addDefaultUserDatasets(
                    datasets, estateId, regionalUser,lastModifiedDatasetKemandoran,lastModifiedDatasetBlok,
                    lastModifiedDatasetTPH, lastModifiedDatasetPemanen, lastModifiedDatasetEstate
                )
            }

            UserRole.GM -> {
                addGMDatasets(
                    datasets,  regionalUser,estateId,lastModifiedDatasetKemandoran,lastModifiedDatasetBlok,
                    lastModifiedDatasetPemanen, lastModifiedDatasetEstate
                )
            }
        }
    }

    private fun addGMDatasets(
        datasets: MutableList<DatasetRequest>,
        regionalId: Int,
        estateId: Any,
        lastModifiedDatasetBlok: String?,
        lastModifiedDatasetKemandoran:String?,
        lastModifiedDatasetTPH: String?,
        lastModifiedDatasetPemanen: String?
    ) {
        AppLogger.d("ksjdlkfjs lkfjsldfj")
        datasets.addAll(
            listOf(
                DatasetRequest(
                    regional = regionalId,
                    lastModified = lastModifiedDatasetBlok,
                    dataset = AppUtils.DatasetNames.blok,
                    jabatan = AppUtils.ListFeatureByRoleUser.GM
                ),
                DatasetRequest(
                    estate = estateId,
                    lastModified = lastModifiedDatasetTPH,
                    dataset = AppUtils.DatasetNames.tph,
                    jabatan = AppUtils.ListFeatureByRoleUser.GM
                ),
                DatasetRequest(
                    regional = regionalId,
                    lastModified = lastModifiedDatasetKemandoran,
                    dataset = AppUtils.DatasetNames.kemandoran,
                    jabatan = AppUtils.ListFeatureByRoleUser.GM
                ),
                DatasetRequest(
                    regional = regionalId,
                    lastModified = lastModifiedDatasetPemanen,
                    dataset = AppUtils.DatasetNames.pemanen,
                    jabatan = AppUtils.ListFeatureByRoleUser.GM
                )
            )
        )
    }

    private fun addKeraniTimbangDatasets(
        datasets: MutableList<DatasetRequest>,
        regionalUser: Int,
        estateId: Any,
        regionalId: Int,
        lastModifiedDatasetEstate: String?,
        lastModifiedDatasetKemandoran:String?,
        lastModifiedDatasetBlok: String?,
        lastModifiedDatasetTPH: String?,
        lastModifiedDatasetPemanen: String?
    ) {
        datasets.addAll(
            listOf(
                DatasetRequest(
                    regional = regionalUser,
                    lastModified = lastModifiedDatasetEstate,
                    dataset = AppUtils.DatasetNames.estate
                ),
                DatasetRequest(
                    regional = regionalUser,
                    lastModified = lastModifiedDatasetBlok,
                    dataset = AppUtils.DatasetNames.blok
                ),
                DatasetRequest(
                    estate = estateId,
                    lastModified = lastModifiedDatasetKemandoran,
                    dataset = AppUtils.DatasetNames.kemandoran
                ),
                DatasetRequest(
                    regional = regionalId,
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
        estateId: Any,
        lastModifiedDatasetKemandoran:String?,
        lastModifiedDatasetBlok: String?,
        lastModifiedDatasetTPH: String?,
        lastModifiedDatasetPemanen: String?,
        lastModifiedDatasetEstate: String?
    ) {
        datasets.addAll(
            listOf(
                DatasetRequest(
                    estate = estateId,
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
                    estate = estateId,
                    lastModified = lastModifiedDatasetKemandoran,
                    dataset = AppUtils.DatasetNames.kemandoran
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
        estateId: Any,
        regionalUser: Int,
        lastModifiedDatasetKemandoran:String?,
        lastModifiedDatasetBlok: String?,
        lastModifiedDatasetTPH: String?,
        lastModifiedDatasetPemanen: String?,
        lastModifiedDatasetEstate: String?
    ) {
        datasets.addAll(
            listOf(
                DatasetRequest(
                    estate = estateId,
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
                    lastModified = lastModifiedDatasetKemandoran,
                    dataset = AppUtils.DatasetNames.kemandoran
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
        userRole: UserRole,
        regionalId: Int,
        estateId: Any,
        afdelingId: Any,
        lastModifiedDatasetJenisTPH: String?,
        lastModifiedDatasetKemandoran: String?,
        lastModifiedDatasetTransporter: String?,
        lastModifiedDatasetKendaraan: String?,
        lastModifiedSettingJSON: String?
    ) {
        // Get the jabatan string based on user role
        val jabatanValue = when (userRole) {
            UserRole.KERANI_TIMBANG -> AppUtils.ListFeatureByRoleUser.KeraniTimbang
            UserRole.KERANI_PANEN -> AppUtils.ListFeatureByRoleUser.KeraniPanen
            UserRole.MANDOR_1 -> AppUtils.ListFeatureByRoleUser.Mandor1
            UserRole.MANDOR_PANEN -> AppUtils.ListFeatureByRoleUser.MandorPanen
            UserRole.ASISTEN -> AppUtils.ListFeatureByRoleUser.Asisten
            UserRole.ASKEP -> AppUtils.ListFeatureByRoleUser.ASKEP
            UserRole.MANAGER -> AppUtils.ListFeatureByRoleUser.Manager
            UserRole.GM -> AppUtils.ListFeatureByRoleUser.GM
            UserRole.OTHER -> null
        }

        datasets.addAll(
            listOf(
                DatasetRequest(
                    regional = regionalId,
                    lastModified = null,
                    dataset = AppUtils.DatasetNames.mill,
                    jabatan = jabatanValue
                ),
                DatasetRequest(
                    lastModified = lastModifiedDatasetJenisTPH,
                    dataset = AppUtils.DatasetNames.jenisTPH,
                    jabatan = jabatanValue
                ),
                DatasetRequest(
                    lastModified = lastModifiedDatasetTransporter,
                    dataset = AppUtils.DatasetNames.transporter,
                    jabatan = jabatanValue
                ),
                DatasetRequest(
                    lastModified = lastModifiedDatasetKendaraan,
                    dataset = AppUtils.DatasetNames.kendaraan,
                    jabatan = jabatanValue
                ),
                DatasetRequest(
                    lastModified = lastModifiedSettingJSON,
                    dataset = AppUtils.DatasetNames.settingJSON,
                    jabatan = jabatanValue
                ),
                DatasetRequest(
                    lastModified = null,
                    idUser = prefManager.idUserLogin,
                    dataset = AppUtils.DatasetNames.checkAppVersion,
                    jabatan = jabatanValue
                )
            )
        )
    }
}