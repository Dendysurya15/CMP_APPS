package com.cbi.mobile_plantation.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.InputStreamReader

data class DetectionMetrics(
    val inferenceTime: Long,
    val preprocessingTime: Long,
    val postprocessingTime: Long,
    val totalTime: Long,
    val fps: Float,
    val classCounts: Map<String, Int>
)

data class DetectionSettings(
    var confidenceThreshold: Float = 0.3f,
    var iouThreshold: Float = 0.5f,
    var boxTransparency: Float = 1.0f
)

class FFBDetector(
    private val context: Context,
    private val modelPath: String = Constants.MODEL_PATH,
    private val labelPath: String? = Constants.LABELS_PATH,
    private val detectorListener: DetectorListener
) {

    private var interpreter: Interpreter? = null
    var labels = mutableListOf<String>()

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0

    val settings = DetectionSettings()

    private var frameCount = 0
    private var lastFpsTime = SystemClock.uptimeMillis()
    private var currentFps = 0f
    private val fpsUpdateInterval = 1000L

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    private var isInitialized = false

    fun initialize(useGPU: Boolean = false): Boolean {
        return try {
            val compatList = CompatibilityList()

            val options = Interpreter.Options().apply {
                if (useGPU && compatList.isDelegateSupportedOnThisDevice) {
                    val delegateOptions = compatList.bestOptionsForThisDevice
                    this.addDelegate(GpuDelegate(delegateOptions))
                } else {
                    this.setNumThreads(4)
                }
            }

            val model = FileUtil.loadMappedFile(context, modelPath)
            interpreter = Interpreter(model, options)

            val inputShape = interpreter?.getInputTensor(0)?.shape()
            val outputShape = interpreter?.getOutputTensor(0)?.shape()

            // Load labels
            loadLabels()

            if (inputShape != null) {
                tensorWidth = inputShape[1]
                tensorHeight = inputShape[2]

                if (inputShape[1] == 3) {
                    tensorWidth = inputShape[2]
                    tensorHeight = inputShape[3]
                }
            }

            if (outputShape != null) {
                numChannel = outputShape[1]
                numElements = outputShape[2]
            }

            Log.d("FFBDetector", "Model loaded successfully")
            Log.d("FFBDetector", "Input shape: ${inputShape?.contentToString()}")
            Log.d("FFBDetector", "Output shape: ${outputShape?.contentToString()}")
            Log.d("FFBDetector", "Labels count: ${labels.size}")

            isInitialized = true
            true
        } catch (e: Exception) {
            Log.e("FFBDetector", "Failed to initialize detector", e)
            false
        }
    }

    private fun loadLabels() {
        labels.clear()
        try {
            labelPath?.let { path ->
                val inputStream = context.assets.open(path)
                val reader = BufferedReader(InputStreamReader(inputStream))
                reader.useLines { lines ->
                    lines.forEach { line ->
                        if (line.trim().isNotEmpty()) {
                            labels.add(line.trim())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FFBDetector", "Error loading labels", e)
            // Default labels
            labels.addAll(listOf("FFB", "Ripe", "Unripe", "Overripe"))
        }
    }

    fun updateSettings(confidenceThreshold: Float, iouThreshold: Float, boxTransparency: Float) {
        settings.confidenceThreshold = confidenceThreshold.coerceIn(0.1f, 0.9f)
        settings.iouThreshold = iouThreshold.coerceIn(0.1f, 0.9f)
        settings.boxTransparency = boxTransparency.coerceIn(0.1f, 1.0f)
    }

    fun detect(frame: Bitmap): Boolean {
        if (!isInitialized || interpreter == null) {
            Log.w("FFBDetector", "Detector not initialized")
            return false
        }

        if (tensorWidth == 0 || tensorHeight == 0 || numChannel == 0 || numElements == 0) {
            Log.w("FFBDetector", "Invalid tensor dimensions")
            return false
        }

        return try {
            val totalStartTime = SystemClock.uptimeMillis()

            // Preprocessing
            val preprocessStartTime = SystemClock.uptimeMillis()
            val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)

            val tensorImage = TensorImage(INPUT_IMAGE_TYPE)
            tensorImage.load(resizedBitmap)
            val processedImage = imageProcessor.process(tensorImage)
            val imageBuffer = processedImage.buffer
            val preprocessingTime = SystemClock.uptimeMillis() - preprocessStartTime

            // Inference
            val inferenceStartTime = SystemClock.uptimeMillis()
            val output = TensorBuffer.createFixedSize(
                intArrayOf(1, numChannel, numElements),
                OUTPUT_IMAGE_TYPE
            )
            interpreter?.run(imageBuffer, output.buffer)
            val inferenceTime = SystemClock.uptimeMillis() - inferenceStartTime

            // Postprocessing
            val postprocessStartTime = SystemClock.uptimeMillis()
            val bestBoxes = bestBox(output.floatArray)
            val postprocessingTime = SystemClock.uptimeMillis() - postprocessStartTime

            val totalTime = SystemClock.uptimeMillis() - totalStartTime
            val fps = calculateFps()

            val classCounts = bestBoxes?.groupingBy { it.clsName }?.eachCount() ?: emptyMap()

            val metrics = DetectionMetrics(
                inferenceTime = inferenceTime,
                preprocessingTime = preprocessingTime,
                postprocessingTime = postprocessingTime,
                totalTime = totalTime,
                fps = fps,
                classCounts = classCounts
            )

            if (bestBoxes.isNullOrEmpty()) {
                detectorListener.onEmptyDetect(metrics)
            } else {
                detectorListener.onDetect(bestBoxes, metrics)
            }

            true
        } catch (e: Exception) {
            Log.e("FFBDetector", "Error during detection", e)
            false
        }
    }

    private fun bestBox(array: FloatArray): List<BoundingBox>? {
        val boundingBoxes = mutableListOf<BoundingBox>()

        for (c in 0 until numElements) {
            var maxConf = settings.confidenceThreshold
            var maxIdx = -1
            var j = 4
            var arrayIdx = c + numElements * j

            while (j < numChannel) {
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            if (maxConf > settings.confidenceThreshold) {
                val classIndex = maxIdx.coerceIn(0, labels.size - 1)
                val clsName = if (labels.isNotEmpty()) labels[classIndex] else "Unknown"

                val cx = array[c]
                val cy = array[c + numElements]
                val w = array[c + numElements * 2]
                val h = array[c + numElements * 3]
                val x1 = cx - (w / 2F)
                val y1 = cy - (h / 2F)
                val x2 = cx + (w / 2F)
                val y2 = cy + (h / 2F)

                if (x1 in 0F..1F && y1 in 0F..1F && x2 in 0F..1F && y2 in 0F..1F) {
                    boundingBoxes.add(
                        BoundingBox(
                            x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                            cx = cx, cy = cy, w = w, h = h,
                            cnf = maxConf, cls = classIndex, clsName = clsName
                        )
                    )
                }
            }
        }

        return if (boundingBoxes.isEmpty()) null else applyNMS(boundingBoxes)
    }

    private fun applyNMS(boxes: List<BoundingBox>): MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while (sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= settings.iouThreshold) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

    private fun calculateFps(): Float {
        frameCount++
        val currentTime = SystemClock.uptimeMillis()
        val elapsed = currentTime - lastFpsTime

        if (elapsed >= fpsUpdateInterval) {
            currentFps = (frameCount * 1000f) / elapsed
            frameCount = 0
            lastFpsTime = currentTime
        }

        return currentFps
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
    }

    interface DetectorListener {
        fun onEmptyDetect(metrics: DetectionMetrics)
        fun onDetect(boundingBoxes: List<BoundingBox>, metrics: DetectionMetrics)
    }

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
    }
}