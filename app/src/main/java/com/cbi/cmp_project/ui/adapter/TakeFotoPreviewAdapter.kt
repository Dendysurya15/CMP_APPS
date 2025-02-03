package com.cbi.cmp_project.ui.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.repository.CameraRepository
import com.cbi.cmp_project.ui.viewModel.CameraViewModel
import com.google.android.material.snackbar.Snackbar
import java.io.File

class TakeFotoPreviewAdapter(
    private val itemCount: Int,
    private val cameraViewModel: CameraViewModel,
    private val context: Context,
    private val featureName: String?
) : RecyclerView.Adapter<TakeFotoPreviewAdapter.FotoViewHolder>(), CameraRepository.PhotoCallback {

    var onItemClick: ((Int) -> Unit)? = null
    private val comments = mutableListOf<String>()
    private val listFileFoto = mutableMapOf<String, File>()

    init {
        for (i in 0 until itemCount) {
            comments.add("")
        }
    }

    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivAddFoto)
        val commentTextView: TextView = itemView.findViewById(R.id.etPhotoComment)
        val titleCommentTextView: TextView = itemView.findViewById(R.id.titleComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.take_and_preview_foto_layout, parent, false)
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        holder.titleCommentTextView.text = "Komentar Foto ${position + 1}"
        holder.commentTextView.text = comments[position]

//
//        // Show comment fields if photo exists (except for first position)
//        if (listFileFoto.containsKey(position.toString())) {
//            if (position > 0) {
//                holder.titleCommentTextView.visibility = View.VISIBLE
//                holder.commentTextView.visibility = View.VISIBLE
//            }
//        }

        holder.commentTextView.setOnClickListener {
            showCommentDialog(position, holder.commentTextView, position + 1)
        }

        holder.itemView.setOnClickListener {
            if (position > 0 && comments[position].isEmpty()) {
                // For positions 1 and 2, require comment first
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(80)
                }

                Toast.makeText(context, "Harap Mengisi Komentar Terlebih Dahulu!", Toast.LENGTH_SHORT).show()

                val background = holder.commentTextView.background as GradientDrawable
                background.setStroke(6, ContextCompat.getColor(context, android.R.color.holo_red_dark))
            } else {
                checkCameraPermission(position, holder)
            }
        }
    }


    private fun handleCameraAction(position: Int, holder: FotoViewHolder) {
        val uniqueKodeFoto = "${position + 1}"

        if (listFileFoto.containsKey(position.toString())) {
            Log.d("TakeFotoAdapter", "Opening existing photo for position $position")
            val existingFile = listFileFoto[position.toString()]!!

            cameraViewModel.openZoomPhotos(existingFile) {
                Log.d("TakeFotoAdapter", "Opening camera to retake photo for position $position")
                cameraViewModel.takeCameraPhotos(
                    uniqueKodeFoto,
                    holder.imageView,
                    position,
                    null,
                    comments[position],
                    uniqueKodeFoto,
                    featureName
                )
                listFileFoto[position.toString()] = existingFile
            }
        } else {
            Log.d("TakeFotoAdapter", "Taking new photo for position $position")
            cameraViewModel.takeCameraPhotos(
                uniqueKodeFoto,
                holder.imageView,
                position,
                null,
                comments[position],
                uniqueKodeFoto,
                featureName
            )
        }
    }

    private fun checkCameraPermission(position: Int, holder: FotoViewHolder) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                handleCameraAction(position, holder)
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                context as Activity,
                android.Manifest.permission.CAMERA
            ) -> {
                showPermissionRationale()
            }

            else -> {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(android.Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

//    private fun handleCameraAction(position: Int, holder: FotoViewHolder) {
//        val uniqueKodeFoto = "${position + 1}"
//
//        if (listFileFoto.containsKey(position.toString())) {
//            Log.d("TakeFotoAdapter", "Opening existing photo for position $position")
//            val existingFile = listFileFoto[position.toString()]!!
//
//            // Open the zoom view
//            cameraViewModel.openZoomPhotos(existingFile) {
//                Log.d("TakeFotoAdapter", "Opening camera to retake photo for position $position")
//
//                // Retake the photo without deleting the existing one
//                cameraViewModel.takeCameraPhotos(
//                    uniqueKodeFoto,
//                    holder.imageView,
//                    position,
//                    null,
//                    comments[position],
//                    uniqueKodeFoto,
//                    featureName
//                )
//
//                // After taking a new photo, update the map only if successful
//                // (this assumes `takeCameraPhotos` handles file creation internally)
//                listFileFoto[position.toString()] = existingFile
//            }
//        } else {
//            Log.d("TakeFotoAdapter", "Taking new photo for position $position")
//            cameraViewModel.takeCameraPhotos(
//                uniqueKodeFoto,
//                holder.imageView,
//                position,
//                null,
//                comments[position],
//                uniqueKodeFoto,
//                featureName
//            )
//        }
//    }


    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }

    private fun showPermissionRationale() {
        if (context is Activity) {
            Snackbar.make(
                context.findViewById(android.R.id.content),
                "Camera permission dibutuhkan untuk mengambil foto",
                Snackbar.LENGTH_INDEFINITE
            ).setAction("Izinkan") {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(android.Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }.show()
        }
    }



    fun addPhotoFile(id: String, file: File) {
        listFileFoto[id] = file
    }

    fun removePhotoFile(id: String) {
        listFileFoto.remove(id)

    }

    override fun getItemCount(): Int {
        return itemCount
    }

    private fun showCommentDialog(position: Int, textView: TextView, indexFoto: Int) {
        // Create an AlertDialog to let the user enter a comment
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Komentar Foto $indexFoto")


        val commentEditText = EditText(context)
        commentEditText.setText(textView.text) // Set the current comment as default
        commentEditText.hint = "Enter your comment here"

        builder.setView(commentEditText)

        builder.setPositiveButton("Simpan") { _, _ ->
            comments[position] = commentEditText.text.toString()
            textView.text = comments[position]

            val background = textView.background as GradientDrawable
            background.setStroke(6, ContextCompat.getColor(context, android.R.color.holo_green_light)) // Set green border
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    override fun onPhotoTaken(photoFile: File, fname: String, resultCode: String, deletePhoto: View?, position: Int, komentar : String?) {
        listFileFoto[position.toString()] = photoFile
        notifyDataSetChanged()

    }
}

