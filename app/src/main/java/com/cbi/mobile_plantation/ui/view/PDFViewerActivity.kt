package com.cbi.mobile_plantation.ui.view

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cbi.mobile_plantation.R
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import java.io.File

class PDFViewerActivity : AppCompatActivity(), OnPageChangeListener, OnLoadCompleteListener {

    private lateinit var pdfView: PDFView
    private var pageNumber = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdfviewer)

        pdfView = findViewById(R.id.pdfView)

        val pdfPath = intent.getStringExtra("PDF_PATH")
        if (pdfPath != null) {
            val file = File(pdfPath)
            if (file.exists()) {
                displayPDF(file)
            } else {
                Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Invalid PDF file", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun displayPDF(file: File) {
        pdfView.fromFile(file)
            .defaultPage(pageNumber)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .onPageChange(this)
            .enableAnnotationRendering(true)
            .onLoad(this)
            .scrollHandle(DefaultScrollHandle(this))
            .load()
    }

    override fun onPageChanged(page: Int, pageCount: Int) {
        pageNumber = page
        title = String.format("%s %s / %s", "Page", page + 1, pageCount)
    }

    override fun loadComplete(nbPages: Int) {
        // PDF loaded successfully
        Log.d("PDFViewer", "PDF loaded with $nbPages pages")
    }
}