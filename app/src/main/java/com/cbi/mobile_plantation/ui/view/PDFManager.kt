package com.cbi.mobile_plantation.ui.view

import android.content.Context
import android.os.Environment
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PDFManager(private val context: Context) {
    private val TAG = "PDFManager"
    private val client = OkHttpClient()

    // Directory for storing PDF files
    private val pdfDir: File by lazy {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "pdfs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    // Interface for download callbacks
    interface DownloadCallback {
        fun onDownloadSuccess(file: File)
        fun onDownloadFailure(message: String)
        fun onDownloadProgress(progress: Int)
    }

    // Download PDF from URL
    fun downloadPDF(url: String, fileName: String, callback: DownloadCallback) {
        val destinationFile = File(pdfDir, fileName)

        // Check if file already exists
        if (destinationFile.exists()) {
            Log.d(TAG, "File already exists: ${destinationFile.absolutePath}")
            callback.onDownloadSuccess(destinationFile)
            return
        }

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onDownloadFailure("Download failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback.onDownloadFailure("Server error: ${response.code}")
                    return
                }

                // Get content length for progress tracking
                val contentLength = response.body?.contentLength() ?: -1L
                val inputStream = response.body?.byteStream() ?: return

                try {
                    val outputStream = FileOutputStream(destinationFile)
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Calculate progress
                        if (contentLength > 0) {
                            val progress = (totalBytesRead * 100 / contentLength).toInt()
                            callback.onDownloadProgress(progress)
                        }
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()

                    callback.onDownloadSuccess(destinationFile)

                } catch (e: Exception) {
                    destinationFile.delete() // Clean up partial file
                    callback.onDownloadFailure("Error saving file: ${e.message}")
                }
            }
        })
    }

    // Get local PDF file if it exists
    fun getLocalPDFFile(fileName: String): File? {
        val file = File(pdfDir, fileName)
        return if (file.exists()) file else null
    }

    // Method tambahan untuk cek status download
    fun isPDFDownloaded(fileName: String): Boolean {
        return getLocalPDFFile(fileName) != null
    }

    // Method untuk hapus PDF jika diperlukan
    fun deletePDF(fileName: String): Boolean {
        val file = File(pdfDir, fileName)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}