package com.cbi.mobile_plantation.ui.viewModel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.PemanenFaceEntity
import com.cbi.mobile_plantation.data.model.PemanenPanenInfo
import com.cbi.mobile_plantation.utils.FaceRecognitionHelper
import com.cbi.mobile_plantation.utils.face.FaceEmbeddingModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class IdentifyPemanenViewModel(application: Application) : AndroidViewModel(application) {

  enum class EnrollStep {
    FRONT,
    TEST
  }

  data class EnrollSession(
    val karyawan: KaryawanModel,
    val step: EnrollStep,
    val frontEmbedding: FloatArray? = null
  ) {
    fun withStep(step: EnrollStep): EnrollSession = copy(step = step)

    fun withSample(step: EnrollStep, embedding: FloatArray): EnrollSession = when (step) {
      EnrollStep.FRONT -> copy(frontEmbedding = embedding, step = EnrollStep.TEST)
      EnrollStep.TEST -> this
    }
  }

  private val database = AppDatabase.getDatabase(application)
  private val karyawanDao = database.karyawanDao()
  private val pemanenFaceDao = database.pemanenFaceDao()
  private val panenDao = database.panenDao()

  private val backendDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

  private val _pemanenList = MutableLiveData<List<KaryawanModel>>(emptyList())
  val pemanenList: LiveData<List<KaryawanModel>> = _pemanenList

  private val _enrolledCount = MutableLiveData(0)
  val enrolledCount: LiveData<Int> = _enrolledCount

  private val _identifyResult = MutableLiveData<IdentifyState>()
  val identifyResult: LiveData<IdentifyState> = _identifyResult

  private val _enrollSession = MutableLiveData<EnrollSession?>(null)
  val enrollSession: LiveData<EnrollSession?> = _enrollSession

  private val _enrollEvent = MutableLiveData<EnrollEvent>()
  val enrollEvent: LiveData<EnrollEvent> = _enrollEvent

  private val _selectedPanenDate = MutableLiveData(getYesterdayDate())
  val selectedPanenDate: LiveData<String> = _selectedPanenDate

  private val _panenInfo = MutableLiveData<PemanenPanenInfo?>()
  val panenInfo: LiveData<PemanenPanenInfo?> = _panenInfo

  sealed class IdentifyState {
    data class Success(val result: FaceRecognitionHelper.FaceMatchResult) : IdentifyState()
    data class NotFound(val message: String) : IdentifyState()
    data class Error(val message: String) : IdentifyState()
    object Processing : IdentifyState()
  }

  sealed class EnrollEvent {
    object Processing : EnrollEvent()
    data class StepCaptured(val completedStep: EnrollStep) : EnrollEvent()
    object TestPassed : EnrollEvent()
    data class Error(val message: String) : EnrollEvent()
    data class TestFailed(val message: String) : EnrollEvent()
  }

  fun getYesterdayDate(): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    return backendDateFormat.format(calendar.time)
  }

  fun setSelectedPanenDate(date: String) {
    _selectedPanenDate.value = date
  }

  fun loadPanenInfo(nik: String, date: String) {
    viewModelScope.launch(Dispatchers.IO) {
      val records = panenDao.getPanenByNikAndDate(nik.trim(), date)
      val totalTo = records.sumOf { parseToFromJjgJson(it.panen.jjg_json) }
      val bloks = records.mapNotNull { relation ->
        relation.tph?.blok_kode?.trim()?.takeIf { it.isNotEmpty() }
      }.distinct().sorted()

      val info = PemanenPanenInfo(
        totalJanjangTo = totalTo,
        blokList = bloks,
        queryDate = date
      )
      withContext(Dispatchers.Main) {
        _panenInfo.value = info
      }
    }
  }

  fun clearPanenInfo() {
    _panenInfo.value = null
  }

  private fun parseToFromJjgJson(jjgJson: String): Int {
    if (jjgJson.isBlank()) return 0
    return try {
      JSONObject(jjgJson).optInt("TO", 0)
    } catch (_: Exception) {
      0
    }
  }

  fun loadPemanenData(modelPrefix: String) {
    viewModelScope.launch(Dispatchers.IO) {
      val karyawanList = karyawanDao.getAllKaryawan()
      val enrolled = pemanenFaceDao.getCountByEmbeddingPrefix("$modelPrefix%")
      withContext(Dispatchers.Main) {
        _pemanenList.value = karyawanList
        _enrolledCount.value = enrolled
      }
    }
  }

  fun startEnrollSession(karyawan: KaryawanModel) {
    _enrollSession.value = EnrollSession(karyawan = karyawan, step = EnrollStep.FRONT)
  }

  fun cancelEnrollSession() {
    _enrollSession.value = null
  }

  fun isEnrollActive(): Boolean = _enrollSession.value != null

  private data class CaptureSampleResult(
    val event: EnrollEvent,
    val updatedSession: EnrollSession?
  )

  fun processEnrollCapture(bitmap: Bitmap, rotationDegrees: Int) {
    val session = _enrollSession.value ?: return

    viewModelScope.launch {
      _enrollEvent.value = EnrollEvent.Processing
      try {
        when (session.step) {
          EnrollStep.FRONT -> {
            val result = withContext(Dispatchers.Default) {
              captureSample(session, bitmap, rotationDegrees)
            }
            result.updatedSession?.let { _enrollSession.value = it }
            _enrollEvent.value = result.event
          }

          EnrollStep.TEST -> {
            val event = withContext(Dispatchers.Default) {
              runEnrollTest(session, bitmap, rotationDegrees)
            }
            if (event is EnrollEvent.TestPassed) {
              _enrollSession.value = null
              FaceEmbeddingModelManager.init(getApplication())
              val prefix = FaceEmbeddingModelManager.getActiveModelType().versionPrefix
              _enrolledCount.value = withContext(Dispatchers.IO) {
                pemanenFaceDao.getCountByEmbeddingPrefix("$prefix%")
              }
            }
            _enrollEvent.value = event
          }
        }
      } catch (e: Exception) {
        _enrollEvent.value = EnrollEvent.Error(e.message ?: "Gagal memproses foto pendaftaran")
      }
    }
  }

  private suspend fun captureSample(
    session: EnrollSession,
    bitmap: Bitmap,
    rotationDegrees: Int
  ): CaptureSampleResult {
    when (val validation = FaceRecognitionHelper.validateSingleFace(bitmap, rotationDegrees)) {
      FaceRecognitionHelper.SingleFaceValidation.NoFace ->
        return CaptureSampleResult(
          EnrollEvent.Error("Wajah tidak terdeteksi. Pastikan wajah berada di dalam bingkai."),
          null
        )

      is FaceRecognitionHelper.SingleFaceValidation.MultipleFaces ->
        return CaptureSampleResult(
          EnrollEvent.Error(
            "Terdeteksi ${validation.count} wajah. Hanya satu wajah yang boleh ada di bingkai."
          ),
          null
        )

      is FaceRecognitionHelper.SingleFaceValidation.Valid -> {
        val embedding = FaceRecognitionHelper.extractEmbedding(bitmap, validation.face, rotationDegrees)
          ?: return CaptureSampleResult(
            EnrollEvent.Error("Tidak dapat membaca fitur wajah. Coba ulangi dengan pencahayaan lebih baik."),
            null
          )

        val completedStep = session.step
        return CaptureSampleResult(
          EnrollEvent.StepCaptured(completedStep),
          session.withSample(completedStep, embedding)
        )
      }
    }
  }

  private suspend fun runEnrollTest(
    session: EnrollSession,
    bitmap: Bitmap,
    rotationDegrees: Int
  ): EnrollEvent {
    val reference = session.frontEmbedding
      ?: return EnrollEvent.Error("Data wajah belum lengkap. Ulangi pendaftaran dari awal.")

    when (val validation = FaceRecognitionHelper.validateSingleFace(bitmap, rotationDegrees)) {
      FaceRecognitionHelper.SingleFaceValidation.NoFace ->
        return EnrollEvent.Error("Wajah tidak terdeteksi. Hadapkan wajah ke kamera untuk uji identifikasi.")

      is FaceRecognitionHelper.SingleFaceValidation.MultipleFaces ->
        return EnrollEvent.Error(
          "Terdeteksi ${validation.count} wajah. Hanya satu wajah yang boleh ada di bingkai."
        )

      is FaceRecognitionHelper.SingleFaceValidation.Valid -> {
        val probe = FaceRecognitionHelper.extractEmbedding(bitmap, validation.face, rotationDegrees)
          ?: return EnrollEvent.Error("Tidak dapat membaca fitur wajah untuk uji identifikasi.")

        if (!FaceRecognitionHelper.matchesEnrollment(getApplication(), probe, reference)) {
          return EnrollEvent.TestFailed(
            "Uji identifikasi gagal. Pastikan pencahayaan cukup dan posisi wajah sama seperti saat pendaftaran."
          )
        }

        saveEnrolledFace(session.karyawan, reference)
        return EnrollEvent.TestPassed
      }
    }
  }

  private suspend fun saveEnrolledFace(karyawan: KaryawanModel, embedding: FloatArray) {
    val kemandoranName = karyawan.kemandoran_id?.let { id ->
      database.kemandoranDao().getKemandoranByTheId(id)?.nama
    }.orEmpty()

    val entity = PemanenFaceEntity(
      karyawan_id = karyawan.id ?: throw IllegalStateException("ID karyawan tidak valid"),
      nik = karyawan.nik.orEmpty(),
      nama = karyawan.nama.orEmpty(),
      kemandoran_nama = kemandoranName,
      embedding = FaceRecognitionHelper.embeddingToString(embedding),
      updated_at = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    )
    pemanenFaceDao.insertOrUpdate(entity)
  }

  private suspend fun processIdentifyEmbedding(embedding: FloatArray): IdentifyState {
    val enrolledFaces = pemanenFaceDao.getAll()
    if (enrolledFaces.isEmpty()) {
      return IdentifyState.NotFound(
        "Belum ada data wajah pemanen terdaftar. Daftarkan wajah pemanen terlebih dahulu."
      )
    }

    val metadata = enrolledFaces.associate {
      it.karyawan_id to Triple(it.nik, it.nama, it.kemandoran_nama)
    }
    val candidates = enrolledFaces.mapNotNull { entity ->
      val storedEmbedding = FaceRecognitionHelper.stringToEmbedding(entity.embedding)
        ?: return@mapNotNull null
      Triple(entity.karyawan_id, entity.nik, storedEmbedding)
    }

    if (candidates.isEmpty()) {
      return IdentifyState.NotFound(
        "Belum ada wajah terdaftar dengan MobileFaceNet. Daftarkan ulang wajah pemanen."
      )
    }

    val match = FaceRecognitionHelper.findBestMatch(getApplication(), embedding, candidates, metadata)
      ?: return IdentifyState.NotFound(
        "Pemanen tidak dikenali. Pastikan wajah sudah terdaftar dan pencahayaan cukup."
      )

    return IdentifyState.Success(match)
  }

  fun identifyFromBitmap(bitmap: Bitmap, rotationDegrees: Int) {
    viewModelScope.launch {
      _identifyResult.value = IdentifyState.Processing
      try {
        val result = withContext(Dispatchers.Default) {
          when (val validation = FaceRecognitionHelper.validateSingleFace(bitmap, rotationDegrees)) {
            FaceRecognitionHelper.SingleFaceValidation.NoFace ->
              return@withContext IdentifyState.Error(
                "Wajah tidak terdeteksi. Pastikan wajah berada di dalam bingkai dengan pencahayaan cukup."
              )

            is FaceRecognitionHelper.SingleFaceValidation.MultipleFaces ->
              return@withContext IdentifyState.Error(
                "Terdeteksi ${validation.count} wajah. Identifikasi hanya untuk satu wajah dalam bingkai."
              )

            is FaceRecognitionHelper.SingleFaceValidation.Valid -> {
              val embedding = FaceRecognitionHelper.extractEmbedding(
                bitmap,
                validation.face,
                rotationDegrees
              ) ?: return@withContext IdentifyState.Error(
                "Wajah tidak terdeteksi atau tidak dapat dibaca. Pastikan wajah berada di dalam bingkai dengan pencahayaan cukup."
              )

              processIdentifyEmbedding(embedding)
            }
          }
        }
        if (result is IdentifyState.Success) {
          val panenDate = _selectedPanenDate.value ?: getYesterdayDate()
          loadPanenInfo(result.result.nik, panenDate)
        }
        _identifyResult.value = result
      } catch (e: Exception) {
        _identifyResult.value = IdentifyState.Error(e.message ?: "Gagal mengidentifikasi wajah")
      }
    }
  }

  class Factory(private val application: Application) :
    androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(IdentifyPemanenViewModel::class.java)) {
        return IdentifyPemanenViewModel(application) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class")
    }
  }
}
