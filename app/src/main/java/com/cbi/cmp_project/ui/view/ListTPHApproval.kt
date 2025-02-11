package com.cbi.cmp_project.ui.view

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.repository.AppRepository
import com.cbi.cmp_project.data.model.TphRvData
import com.cbi.cmp_project.ui.adapter.TPHRvAdapter
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.PrefManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ListTPHApproval : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TPHRvAdapter
    private val repository by lazy { AppRepository(this) }

    private var featureName: String? = null

    companion object {
        const val EXTRA_QR_RESULT = "scannedResult"
        private const val TAG = "ListTPHApproval"
    }

    private var userName: String? = null
    private var estateName: String? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null

    private lateinit var data: List<TphRvData>
    private lateinit var saveData: List<TphRvData>
    val _saveDataPanenState = MutableStateFlow<SaveDataPanenState>(SaveDataPanenState.Loading)

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_panen_tbs)

        setupRecyclerView()
        processQRResult()
        val flCheckBoxTableHeaderLayout =
            findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
        flCheckBoxTableHeaderLayout.visibility = View.GONE
        val constraintLayout = findViewById<ConstraintLayout>(R.id.clParentListPanen)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
//        val filterSection = findViewById<LinearLayout>(R.id.filterSection)
        constraintSet.connect(
            R.id.filterSection, ConstraintSet.TOP,
            R.id.navbarPanenList, ConstraintSet.BOTTOM
        )
        constraintSet.applyTo(constraintLayout)
        val list_menu_upload_data = findViewById<LinearLayout>(R.id.list_menu_upload_data)
        list_menu_upload_data.visibility = View.GONE

        val btnGenerateQRTPH: FloatingActionButton = findViewById(R.id.btnGenerateQRTPH)
        btnGenerateQRTPH.setImageResource(R.drawable.baseline_save_24)
        btnGenerateQRTPH.imageTintList = ColorStateList.valueOf(Color.WHITE)
        // Convert 20dp to pixels
        val marginInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics
        ).toInt()
        // Get the existing layout params and cast it to MarginLayoutParams
        val params = btnGenerateQRTPH.layoutParams as ViewGroup.MarginLayoutParams
        // Set margins (left, top, right, bottom)
        params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx)
        // Apply the updated params
        btnGenerateQRTPH.layoutParams = params

        featureName = intent.getStringExtra("FEATURE_NAME")
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        val userSection = findViewById<TextView>(R.id.userSection)

        val prefManager = PrefManager(this)
        userName = try {
            prefManager.getUserNameLogin("user_name")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user_name", e)
            "null"
        }
        estateName = try {
            prefManager.getEstateUserLogin("estate_name")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting estate_name", e)
            "null"
        }
        jabatanUser = try {
            prefManager.getJabatanUserLogin("jabatan_name")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting jabatan_name", e)
            "null"
        }
        AppUtils.setupUserHeader(
            userName = userName,
            jabatanUser = jabatanUser,
            estateName = estateName,
            afdelingUser = afdelingUser,
            userSection = userSection,
            featureName = featureName,
            tvFeatureName = tvFeatureName
        )

        btnGenerateQRTPH.setOnClickListener {
            AlertDialogUtility.withTwoActions(
                this,
                "Simpan",
                "Apakah anda ingin menyimpan data ini?",
                getString(R.string.confirmation_dialog_description),
                "warning.json"
            ) {
                lifecycleScope.launch {
                    try {
                        _saveDataPanenState.value = SaveDataPanenState.Loading

                        val result = repository.saveTPHDataList(saveData)

                        result.fold(
                            onSuccess = { savedIds ->
                                _saveDataPanenState.value = SaveDataPanenState.Success(savedIds)
                                Toasty.success(
                                    this@ListTPHApproval,
                                    "Data berhasil disimpan",
                                    Toast.LENGTH_LONG,
                                    true
                                ).show()
                                startActivity(
                                    Intent(
                                        this@ListTPHApproval,
                                        HomePageActivity::class.java
                                    )
                                )
                                finish()
                            },
                            onFailure = { exception ->
                                _saveDataPanenState.value = SaveDataPanenState.Error(
                                    exception.message ?: "Unknown error occurred"
                                )
                                if (exception.message?.contains("Duplicate data found") == true) {
                                    AlertDialogUtility.withSingleAction(
                                        this@ListTPHApproval,
                                        "OK",
                                        "Data duplikat, anda telah melakukan scan untuk data panen ini!",
                                        "Error: ${exception.message}",
                                        "warning.json"
                                    ) {
                                    }
                                } else {
                                    Toasty.error(
                                        this@ListTPHApproval,
                                        "Error: ${exception.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                    } catch (e: Exception) {
                        _saveDataPanenState.value = SaveDataPanenState.Error(
                            e.message ?: "Unknown error occurred"
                        )
                    }
                }
            }
        }

    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvTableData)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TPHRvAdapter(emptyList())
        recyclerView.adapter = adapter
    }

    private fun processQRResult() {
        val qrResult = intent.getStringExtra(EXTRA_QR_RESULT).orEmpty()
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val jsonStr = AppUtils.readJsonFromEncryptedBase64Zip(qrResult)
                    jsonStr?.let {
                        data = parseTphData(it)
                        withContext(Dispatchers.Main) {
                            adapter.updateList(data)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing QR result", e)
                // Consider showing an error message to the user
            }
        }
    }

    private suspend fun parseTphData(jsonString: String): List<TphRvData> =
        withContext(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject(jsonString)
                val tph0String = jsonObject.getString("tph_0")
                Log.d(TAG, "tph0String: $tph0String")

                val parsedEntries = tph0String.split(";").mapNotNull { entry ->
                    if (entry.isBlank()) return@mapNotNull null

                    val parts = entry.split(",")
                    if (parts.size != 3) return@mapNotNull null

                    try {
                        val idtph = parts[0].toInt()
                        Log.d(TAG, "Processing idtph: $idtph")

                        val tphInfo = try {
                            repository.getTPHAndBlokInfo(idtph)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting TPH info for idtph $idtph", e)
                            null
                        }

                        val displayName = tphInfo?.blokKode ?: "Unknown"
                        val datetime = parts[1].split(" ")[1]
                        val jjg = parts[2].toInt()

                        // Create display data
                        val displayData = TphRvData(
                            namaBlok = displayName,
                            noTPH = try {
                                tphInfo!!.tphNomor.toInt()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing tphNomor: ${tphInfo!!.tphNomor}", e)
                                0
                            },
                            time = datetime,
                            jjg = jjg
                        )

                        // Create save data with original values
                        val saveData = TphRvData(
                            namaBlok = parts[0], // Original ID as namaBlok
                            noTPH = idtph,
                            time = parts[1], // Original full datetime
                            jjg = jjg
                        )

                        Pair(displayData, saveData)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing entry: $entry", e)
                        null
                    }
                }

                // Separate display and save data
                data = parsedEntries.map { it.first }
                saveData = parsedEntries.map { it.second }

                return@withContext data
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing JSON", e)
                data = emptyList()
                saveData = emptyList()
                return@withContext emptyList()
            }
        }

    sealed class SaveDataPanenState {
        object Loading : SaveDataPanenState()
        data class Success(val savedIds: List<Long>) : SaveDataPanenState()
        data class Error(val message: String) : SaveDataPanenState()
    }
}