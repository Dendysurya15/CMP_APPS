package com.cbi.mobile_plantation.ui.viewModel

import android.app.Application
import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cbi.mobile_plantation.data.repository.CameraRepository
import com.cbi.mobile_plantation.data.repository.CameraRepository.CameraType // Import the enum
import com.cbi.mobile_plantation.ffbcounter.BoundingBox
import com.cbi.mobile_plantation.ffbcounter.Constants
import com.cbi.mobile_plantation.ffbcounter.DetectionMetrics
import com.cbi.mobile_plantation.ffbcounter.Detector
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class CameraViewModel(private val cameraRepository: CameraRepository) : ViewModel() {

    private var detector: Detector? = null

    private val _liveCount = kotlinx.coroutines.flow.MutableStateFlow(0)
    val liveCount = _liveCount.asStateFlow()

    private val _lockedCount = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)
    val lockedCount = _lockedCount.asStateFlow()

    // optional: small median filter
    private val lastCounts: java.util.ArrayDeque<Int> = java.util.ArrayDeque()


    // Updated function with cameraType parameter, defaulting to BACK camera
    fun takeCameraPhotos(
        context: Context,
        resultCode: String,
        imageView: ImageView,
        pageForm: Int,
        deletePhoto: View?,
        komentar: String,
        kodeFoto: String,
        featureName: String?,
        latitude: Double? = null,
        longitude: Double? = null,
        sourceFoto: String,
        cameraType: CameraType = CameraType.BACK // Default to back camera
    ) {
        cameraRepository.takeCameraPhotos(
            context,
            resultCode,
            imageView,
            pageForm,
            deletePhoto,
            komentar,
            kodeFoto,
            featureName,
            latitude,
            longitude,
            sourceFoto,
            cameraType
        )
    }

    fun statusCamera(): Boolean = cameraRepository.statusCamera()

    fun closeCamera() {
        cameraRepository.closeCamera()
    }

    fun openZoomPhotos(file: File, position: String, onChangePhoto: () -> Unit, onDeletePhoto: (String) -> Unit, onClosePhoto: () -> Unit = {}) {
        cameraRepository.openZoomPhotos(file, position, onChangePhoto, onDeletePhoto, onClosePhoto)
    }

    fun isZoomViewVisible(): Boolean {
        return cameraRepository.isZoomViewVisible()
    }

    fun closeZoomView() {
        cameraRepository.closeZoomView()
    }

    fun deletePhotoSelected(fname: String): Boolean {
        return cameraRepository.deletePhotoSelected(fname)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val cameraRepository: CameraRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
                return CameraViewModel(cameraRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    fun initDetector(app: Application) {
        if (detector != null) return
        detector = Detector(
            app.applicationContext,
            Constants.MODEL_PATH,
            Constants.LABELS_PATH,
            object : Detector.DetectorListener {
                override fun onEmptyDetect(m: DetectionMetrics) {}
                override fun onDetect(
                    b: List<BoundingBox>,
                    m: DetectionMetrics
                ) {}
            }
        ) { /* ignore */ }
    }

    override fun onCleared() {
        super.onCleared()
        detector = null
    }

    fun onRealtimeCountFromRepo(count: Int) {
        // smooth (median over last 7 frames)
        lastCounts.addLast(count)
        if (lastCounts.size > 7) lastCounts.removeFirst()
        val sorted = lastCounts.sorted()
        val median = sorted[sorted.size / 2]

        _liveCount.value = median

        // If you want “lock when stable for N frames”, use this:
        if (_lockedCount.value == null && lastCounts.size >= 7) {
            val same = lastCounts.all { kotlin.math.abs(it - median) <= 1 }
            if (same) _lockedCount.value = median
        }
    }

    fun forceLockNow() { _lockedCount.value = _liveCount.value }
    fun unlock() { _lockedCount.value = null }


}