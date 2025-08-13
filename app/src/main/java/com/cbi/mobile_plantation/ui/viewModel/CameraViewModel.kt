package com.cbi.mobile_plantation.ui.viewModel

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cbi.mobile_plantation.data.repository.CameraRepository
import com.cbi.mobile_plantation.data.repository.CameraRepository.CameraType // Import the enum
import java.io.File

class CameraViewModel(private val cameraRepository: CameraRepository) : ViewModel() {

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
}