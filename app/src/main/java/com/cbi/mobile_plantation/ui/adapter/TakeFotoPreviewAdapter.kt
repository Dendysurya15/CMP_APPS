package com.cbi.mobile_plantation.ui.adapter

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
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.repository.CameraRepository
import com.cbi.mobile_plantation.ui.view.Absensi.FeatureAbsensiActivity
import com.cbi.mobile_plantation.ui.view.panenTBS.FeaturePanenTBSActivity
import com.cbi.mobile_plantation.ui.viewModel.CameraViewModel
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import es.dmoral.toasty.Toasty
import java.io.File

class TakeFotoPreviewAdapter(
    private val maxCount: Int,
    private val cameraViewModel: CameraViewModel,
    private val context: Context,
    private val waterMarkFolder: String?
) : RecyclerView.Adapter<TakeFotoPreviewAdapter.FotoViewHolder>(), CameraRepository.PhotoCallback {

    var onItemClick: ((Int) -> Unit)? = null
    private val comments = mutableListOf<String>()
    private val listFileFoto = mutableMapOf<String, File>()
    private val activeItems = mutableListOf<Boolean>()
    private var maxVisibleSlots = 1  // Track how many slots have been visible

    interface LocationDataProvider {
        fun getCurrentLocationData(): LocationData
        fun getCurrentCoordinates(): Pair<Double?, Double?>
    }

    data class LocationData(
        val estate: String?,
        val afdeling: String?,
        val blok: String?,
        val tph: String?
    )

    var onPhotoDeleted: ((String, Int) -> Unit)? = null

    init {
        // Clear any existing data
        activeItems.clear()
        comments.clear()
        listFileFoto.clear()

        // Start with just one item
        activeItems.add(true)
        comments.add("")
    }

    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivAddFoto)
        val commentTextView: TextView = itemView.findViewById(R.id.tvPhotoComment)
        val titleCommentTextView: TextView = itemView.findViewById(R.id.titleComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.take_and_preview_foto_layout, parent, false)
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        holder.titleCommentTextView.text = "Komentar Foto (Opsional)"
        holder.commentTextView.text = comments[position]

        // Show comment section always
        holder.titleCommentTextView.visibility = View.VISIBLE
        holder.commentTextView.visibility = View.VISIBLE

        // Update image if photo exists
        if (listFileFoto.containsKey(position.toString())) {
            val file = listFileFoto[position.toString()]
            Glide.with(context)
                .load(file)
                .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(R.drawable.baseline_add_a_photo_24)
        }

        holder.commentTextView.setOnClickListener {
            showCommentDialog(position, holder.commentTextView, position + 1)
        }

        // Remove comment requirement check
        holder.itemView.setOnClickListener {
            checkCameraPermission(position, holder)
        }
    }

    private fun showCommentRequiredWarning(holder: FotoViewHolder) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(80)
        }

        Toast.makeText(
            context,
            "Harap Mengisi Komentar Foto Sebelumnya Terlebih Dahulu!",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun getActiveItemCount(): Int = activeItems.size

    private fun hideKeyboardFromView() {
        // Get context from the adapter
        val activity = context as? Activity ?: return

        // Find the currently focused view
        val view = activity.currentFocus ?: View(activity)

        // Hide the keyboard
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }



    private fun handleCameraAction(position: Int, holder: FotoViewHolder) {
        hideKeyboardFromView()

        // Cast to the interface - much cleaner!
        val locationProvider = context as LocationDataProvider
        val locationData = locationProvider.getCurrentLocationData()
        val (currentLat, currentLon) = locationProvider.getCurrentCoordinates()

        // Check if GPS coordinates are available
        if (currentLat == null || currentLon == null) {
            Toast.makeText(
                context,
                "Pastikan GPS mendapatkan titik Koordinat!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        when (waterMarkFolder) {
            AppUtils.WaterMarkFotoDanFolder.WMPanenTPH -> {
                if (locationData.estate.isNullOrEmpty() ||
                    locationData.afdeling.isNullOrEmpty() ||
                    locationData.blok.isNullOrEmpty() ||
                    locationData.tph.isNullOrEmpty()) {

                    AppLogger.d("Estate: ${locationData.estate}")
                    AppLogger.d("Afdeling: ${locationData.afdeling}")
                    AppLogger.d("Blok: ${locationData.blok}")
                    AppLogger.d("TPH: ${locationData.tph}")
                    Toast.makeText(
                        context,
                        "Pastikan sudah mengisi Estate, Afdeling dan No TPH terlebih dahulu!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
            }
            AppUtils.WaterMarkFotoDanFolder.WMAbsensiPanen -> {
                if (locationData.estate.isNullOrEmpty() || locationData.afdeling.isNullOrEmpty()) {
                    AppLogger.d("Estate: ${locationData.estate}")
                    AppLogger.d("Afdeling: ${locationData.afdeling}")
                    Toast.makeText(
                        context,
                        "Pastikan sudah mengisi Estate dan Afdeling terlebih dahulu!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
            }
        }

        val uniqueKodeFoto = "${position + 1}"

        if (listFileFoto.containsKey(position.toString())) {
            val sourceFoto = buildSourceFoto(waterMarkFolder, locationData)
            val existingFile = listFileFoto[position.toString()]!!
            cameraViewModel.openZoomPhotos(
                file = existingFile,
                position = position.toString(),
                onChangePhoto = {
                    cameraViewModel.takeCameraPhotos(
                        context,
                        uniqueKodeFoto,
                        holder.imageView,
                        position,
                        null,
                        comments[position],
                        uniqueKodeFoto,
                        waterMarkFolder,
                        currentLat,
                        currentLon,
                        sourceFoto
                    )
                },
                onDeletePhoto = { pos ->
                    // Get the filename before removing it
                    val fileToDelete = listFileFoto[pos]
                    val fileName = fileToDelete?.name

                    // Remove the photo and clear comment
                    listFileFoto.remove(pos)
                    comments[position.toInt()] = ""
                    holder.imageView.setImageResource(R.drawable.baseline_add_a_photo_24)

                    // Notify the activity about the deletion
                    if (fileName != null) {
                        AppLogger.d("Photo deleted: $fileName at position $position")
                        onPhotoDeleted?.invoke(fileName, position.toInt())
                    }

                    // Since we've reached the max visible count, just update the item's view
                    // Don't remove any slots once we've shown the maximum
                    notifyItemChanged(position)
                }
            )
        } else {
            val sourceFoto = buildSourceFoto(waterMarkFolder, locationData)
            cameraViewModel.takeCameraPhotos(
                context,
                uniqueKodeFoto,
                holder.imageView,
                position,
                null,
                comments[position],
                uniqueKodeFoto,
                waterMarkFolder,
                currentLat,
                currentLon,
                sourceFoto
            )
        }
    }

    private fun buildSourceFoto(featureType: String?, locationData: LocationData): String {
        return when (featureType) {
            AppUtils.WaterMarkFotoDanFolder.WMPanenTPH -> {
                // For TPH feature, we include estate, afdeling, and blok
                "${locationData.estate} ${locationData.afdeling} ${locationData.blok} TPH ${locationData.tph}"
            }
            AppUtils.WaterMarkFotoDanFolder.WMAbsensiPanen -> {
                "${locationData.estate} ${locationData.afdeling}"
            }
            else -> {
                // Default format - just include what we have
                val parts = mutableListOf<String>()
                locationData.estate?.let { if (it.isNotEmpty()) parts.add("Estate: $it") }
                locationData.afdeling?.let { if (it.isNotEmpty()) parts.add("Afdeling: $it") }
                locationData.blok?.let { if (it.isNotEmpty()) parts.add("Blok: $it") }

                parts.joinToString(", ")
            }
        }
    }

    // Helper function to check if there are any photos after given position
    private fun hasPhotosAfterPosition(position: Int): Boolean {
        for (i in position + 1 until activeItems.size) {
            if (listFileFoto.containsKey(i.toString())) {
                return true
            }
        }
        return false
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
                showSnackbarWithSettings("Camera permission required to take photos. Enable it in Settings.")
            }

            else -> {
                // If permission is permanently denied, show settings option
                if (isPermissionPermanentlyDenied()) {
                    AppLogger.d("Permission permanently denied. Redirecting to settings.")
                    showSnackbarWithSettings("Camera permission required to take photos. Enable it in Settings.")
                } else {
                    AppLogger.d("Requesting camera permission")
                    ActivityCompat.requestPermissions(
                        context as Activity,
                        arrayOf(android.Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    // ✅ Check if the user permanently denied permission
    private fun isPermissionPermanentlyDenied(): Boolean {
        val activity = context as Activity
        val sharedPref = activity.getSharedPreferences("permissions_prefs", Context.MODE_PRIVATE)
        val firstRequest = sharedPref.getBoolean("first_camera_request", true)

        if (firstRequest) {
            sharedPref.edit().putBoolean("first_camera_request", false).apply()
            return false
        }

        return !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            android.Manifest.permission.CAMERA
        )
    }

    // ✅ Show Snackbar to open Settings if permanently denied
    private fun showSnackbarWithSettings(message: String) {
        Snackbar.make(
            (context as Activity).findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction("Settings") {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                context.startActivity(intent)
            }
            .show()
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

    private fun addNewItem() {
        if (activeItems.size < maxCount) {
            activeItems.add(true)
            comments.add("")
            notifyItemInserted(activeItems.size - 1)

            // Update our tracking of how many slots have been visible
            if (activeItems.size > maxVisibleSlots) {
                maxVisibleSlots = activeItems.size
            }
        }
    }

    fun addPhotoFile(id: String, file: File) {
        Log.d("TakeFotoAdapter", "addPhotoFile START - id: $id")

        // Check if this position already had a photo
        val hadExistingPhoto = listFileFoto.containsKey(id)

        // Add/Update the photo in the map
        listFileFoto[id] = file

        // Only add new position if we didn't have a photo at this position before
        // and we haven't reached the maximum count yet
        if (!hadExistingPhoto && activeItems.size < maxCount) {
            Log.d("TakeFotoAdapter", "Adding new position. Current size: ${activeItems.size}")
            addNewItem()
            Log.d("TakeFotoAdapter", "New size: ${activeItems.size}")
        }

        // Update the UI
        notifyDataSetChanged()
        Log.d("TakeFotoAdapter", "addPhotoFile END")
    }

    fun removePhotoFile(id: String) {
        // Get the filename before removing it
        val fileToDelete = listFileFoto[id]
        val fileName = fileToDelete?.name

        listFileFoto.remove(id)

        // Notify the activity about the deletion if we have a filename
        if (fileName != null) {
            onPhotoDeleted?.invoke(fileName, id.toInt())
        }

        // If we've already shown the max slots, don't remove any
        if (maxVisibleSlots >= maxCount) {
            notifyItemChanged(id.toInt())
        } else {
            // Otherwise, check if we should remove this section
            val position = id.toInt()
            val shouldRemoveSection =
                position == activeItems.size - 1 && !hasPhotosAfterPosition(position)

            if (shouldRemoveSection && activeItems.size > 1) {
                activeItems.removeAt(position)
                comments.removeAt(position)
                notifyItemRemoved(position)
            } else {
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount(): Int = activeItems.size

    private fun showCommentDialog(position: Int, textView: TextView, indexFoto: Int) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_komentar, null)

        // Set rounded corners for bottom sheet
        view.background = ContextCompat.getDrawable(context, R.drawable.rounded_top_right_left)

        val dialog = BottomSheetDialog(context)
        dialog.setContentView(view)

        // Get screen width
        val displayMetrics = context.resources.displayMetrics
        val width = displayMetrics.widthPixels

        // Set bottom sheet width to 80% of screen width
        dialog.window?.apply {
            setLayout(
                (width * 0.8).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        // Find views in bottom sheet layout
        val titleTextView = view.findViewById<TextView>(R.id.titleDialog)
        val commentEditText = view.findViewById<EditText>(R.id.commentEditText)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        titleTextView.text = "Komentar Foto"
        commentEditText.setText(textView.text)
        commentEditText.hint = "Masukkan komentar disini"

        btnSave.setOnClickListener {
            comments[position] = commentEditText.text.toString()
            textView.text = comments[position]
            val background = textView.background as GradientDrawable

            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        dialog.show()
    }

    fun resetAllSections() {
        // Track the current state before reset
        val previousMaxVisible = maxVisibleSlots

        // Clear all stored data
        listFileFoto.clear()
        comments.clear()
        activeItems.clear()

        // If we once had all slots visible, keep them all visible
        if (previousMaxVisible >= maxCount) {
            for (i in 0 until maxCount) {
                activeItems.add(true)
                comments.add("")
            }
            maxVisibleSlots = maxCount
        } else {
            // Otherwise reset to initial state (one empty section)
            activeItems.add(true)
            comments.add("")
            maxVisibleSlots = 1
        }

        // Notify adapter to refresh
        notifyDataSetChanged()
    }

    override fun onPhotoTaken(
        photoFile: File,
        fname: String,
        resultCode: String,
        deletePhoto: View?,
        position: Int,
        komentar: String?,
        latitude: Double?,
        longitude: Double?
    ) {
        Log.d("TakeFotoAdapter", "onPhotoTaken START - position: $position")

        // Add the photo to our list
        listFileFoto[position.toString()] = photoFile
        Log.d("TakeFotoAdapter", "Photo added to list")

        // Add new position if we can
        if (activeItems.size < maxCount) {
            Log.d("TakeFotoAdapter", "Adding new position. Current size: ${activeItems.size}")
            addNewItem()
            Log.d("TakeFotoAdapter", "New size: ${activeItems.size}")
        }

        // Update the UI
        notifyDataSetChanged()
        Log.d("TakeFotoAdapter", "onPhotoTaken END")
    }

    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }
}
