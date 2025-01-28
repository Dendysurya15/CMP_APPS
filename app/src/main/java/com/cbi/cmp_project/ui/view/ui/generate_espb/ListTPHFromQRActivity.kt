package com.cbi.cmp_project.ui.view.ui.generate_espb

import android.os.Bundle
import android.util.Base64
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cbi.cmp_project.R
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class ListTPHFromQRActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_tphfrom_qractivity)
        val qrResult = intent.getStringExtra(EXTRA_QR_RESULT) ?: ""
        val jsonStr = readJsonFromEncryptedBase64Rar(qrResult)
        findViewById<TextView>(R.id.textViewResult).text = jsonStr
    }

    fun readJsonFromEncryptedBase64Rar(base64String: String): String? {
        return try {
            // Remove header if present
            val base64Data = if (base64String.contains(",")) {
                base64String.substring(base64String.indexOf(",") + 1)
            } else {
                base64String
            }

            val base64Decode = base64Data.replace("5nqHzPKdlILxS9ABpClq","")

            // Decode base64 to bytes
            val decodedBytes = Base64.decode(base64Decode, Base64.DEFAULT)

            // Create RAR archive from bytes
            ByteArrayInputStream(decodedBytes).use { byteStream ->
                Archive(byteStream).use { archive ->
                    // Look for file.json
                    val fileHeader: FileHeader? = archive.fileHeaders.find { it.fileName == "output.json" }

                    if (fileHeader != null) {
                        // Read the JSON content
                        ByteArrayOutputStream().use { outputStream ->
                            archive.extractFile(fileHeader, outputStream)
                            return String(outputStream.toByteArray(), StandardCharsets.UTF_8)
                        }
                    }
                }
            }

            null // Return null if file.json was not found
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        const val EXTRA_QR_RESULT = "scannedResult"
    }
}