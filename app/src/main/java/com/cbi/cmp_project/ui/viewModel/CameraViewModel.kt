package com.cbi.cmp_project.ui.viewModel

import android.view.View
import android.widget.ImageView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cbi.cmp_project.data.repository.CameraRepository
import java.io.File

class CameraViewModel(private val cameraRepository: CameraRepository) : ViewModel() {

    fun takeCameraPhotos(resultCode : String, imageView: ImageView, pageForm : Int, deletePhoto : View?,komentar :String, kodeFoto:String, featureName : String?) {
        cameraRepository.takeCameraPhotos(resultCode, imageView, pageForm, deletePhoto,komentar, kodeFoto, featureName)
    }

    fun statusCamera(): Boolean = cameraRepository.statusCamera()

    fun closeCamera() {
        cameraRepository.closeCamera(   )
    }

    fun openZoomPhotos(file: File,  onChangePhoto: () -> Unit) {
        cameraRepository.openZoomPhotos(file, onChangePhoto)
    }

    fun closeZoomPhotos() {
        cameraRepository.closeZoomPhotos()
    }


    fun deletePhotoSelected(fname :String): Boolean{
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
