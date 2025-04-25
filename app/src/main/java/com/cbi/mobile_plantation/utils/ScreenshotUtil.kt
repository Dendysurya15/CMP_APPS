package com.cbi.mobile_plantation.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaScannerConnection
import android.os.Environment
import android.view.View
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for taking screenshots and saving them to storage
 */
class ScreenshotUtil {
    companion object {
        /**
         * Takes a screenshot of the provided view and saves it to the DCIM directory
         *
         * @param view The view to capture in the screenshot
         * @param fileName The name for the screenshot file (without extension)
         * @param featureName The feature name to use in the directory path
         * @return The saved file or null if unsuccessful
         */
        fun takeScreenshot(view: View, fileName: String, featureName: String): File? {
            try {
                // Create bitmap of the view
                val bitmap = getBitmapFromView(view)

                // Create directory if it doesn't exist
                val directory = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    "CMP-$featureName"
                )

                if (!directory.exists()) {
                    directory.mkdirs()
                }

                // Create file with timestamp to avoid overwrites
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val file = File(directory, "${fileName}_$timestamp.jpg")

                // Save bitmap to file
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                outputStream.close()

                // Make the image appear in gallery
                MediaScannerConnection.scanFile(
                    view.context,
                    arrayOf(file.toString()),
                    arrayOf("image/jpeg"),
                    null
                )

                AppLogger.d("Screenshot saved to: ${file.absolutePath}")
                return file
            } catch (e: Exception) {
                AppLogger.e("Error taking screenshot: ${e.message}")
                e.printStackTrace()
                return null
            }
        }

        /**
         * Converts a view to a bitmap
         */
        private fun getBitmapFromView(view: View): Bitmap {
            // Define bitmap with same size as view
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

            // Draw view content to canvas
            val canvas = Canvas(bitmap)
            view.draw(canvas)

            return bitmap
        }
    }
}