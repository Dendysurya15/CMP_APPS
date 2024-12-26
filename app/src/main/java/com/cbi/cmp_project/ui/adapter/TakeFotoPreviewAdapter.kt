package com.cbi.cmp_project.ui.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.ui.viewModel.CameraViewModel
import java.io.File

class TakeFotoPreviewAdapter(
    private val itemCount: Int,
    private val cameraViewModel: CameraViewModel,
    private val context: Context,
    private val featureName: String?
) : RecyclerView.Adapter<TakeFotoPreviewAdapter.FotoViewHolder>() {

    // Define the click listener interface
    var onItemClick: ((Int) -> Unit)? = null

    // Store the comment text for each item in the adapter
    private val comments = mutableListOf<String>()

    private val listFileFoto = mutableMapOf<String, File>()

    init {
        // Initialize comments list with empty strings
        for (i in 0 until itemCount) {
            comments.add("")
        }
    }

    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivAddFoto)
        val commentTextView: TextView = itemView.findViewById(R.id.etPhotoComment) // Comment TextView
        val titleCommentTextView: TextView = itemView.findViewById(R.id.titleComment) // Title TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.take_and_preview_foto_layout, parent, false)
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        // Dynamically set the titleComment text and color
        holder.titleCommentTextView.text = "Komentar Foto ${position + 1}"

        // Set any existing comment if available
        holder.commentTextView.text = comments[position] // Set the current comment to the TextView

        // Set the click listener for the TextView (to trigger the comment editing)
        holder.commentTextView.setOnClickListener {
            // Create the AlertDialog to update the comment
            showCommentDialog(position, holder.commentTextView, position + 1)
        }

        // Set the click listener for the item view (to trigger the camera action)
        holder.itemView.setOnClickListener {
            if (comments[position].isEmpty()) {

                val background = holder.commentTextView.background as GradientDrawable
                background.setStroke(6, ContextCompat.getColor(context, android.R.color.holo_red_light)) // Set red border

                Toast.makeText(context, "Harap Mengisi Komentar Terlebih Dahulu!", Toast.LENGTH_SHORT).show()

                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(80)
                }
            } else {

                    val uniqueKodeFoto = "${position + 1}"

                    if (listFileFoto.containsKey(position.toString())) {

                        cameraViewModel.openZoomPhotos(listFileFoto[position.toString()]!!) {}
                    } else {
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
        }
    }

    fun addPhotoFile(id: String, file: File) {
        Log.d("testing", "Adding photo with id: $id, file: ${file.path}")
        listFileFoto[id] = file
//        notifyItemChanged(id.toInt())
    }

    fun removePhotoFile(id: String) {
        listFileFoto.remove(id)
//        val position = id.toIntOrNull()?.minus(1) ?: return
//        notifyItemChanged(position)
    }

    override fun getItemCount(): Int {
        return itemCount
    }

    private fun showCommentDialog(position: Int, textView: TextView, indexFoto: Int) {
        // Create an AlertDialog to let the user enter a comment
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Komentar Foto $indexFoto")

        // Create a new EditText for the dialog
        val commentEditText = EditText(context)
        commentEditText.setText(textView.text) // Set the current comment as default
        commentEditText.hint = "Enter your comment here"

        builder.setView(commentEditText)

        builder.setPositiveButton("Simpan") { _, _ ->
            comments[position] = commentEditText.text.toString()
            textView.text = comments[position] // Update the TextView in the RecyclerView

            val background = textView.background as GradientDrawable
            background.setStroke(6, ContextCompat.getColor(context, android.R.color.holo_green_light)) // Set green border
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
}

