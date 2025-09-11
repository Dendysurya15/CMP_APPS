package com.cbi.mobile_plantation.ffbcounter

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.cbi.mobile_plantation.ffbcounter.MetaData.extractNamesFromLabelFile
import com.cbi.mobile_plantation.ffbcounter.MetaData.extractNamesFromMetadata
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

data class DetectionMetrics(
    val inferenceTime: Long,
    val preprocessingTime: Long,
    val postprocessingTime: Long,
    val totalTime: Long,
    val fps: Float,
    val classCounts: Map<String, Int> // Class name to count mapping
)

// Updated DetectionSettings data class in Detector.kt
data class DetectionSettings(
    var confidenceThreshold: Float = 0.3f,
    var iouThreshold: Float = 0.5f,
    var boxTransparency: Float = 1.0f // 0.0 (transparent) to 1.0 (opaque)
)

class Detector(
    private val context: Context,
    private val modelPath: String,
    private val labelPath: String?,
    private val detectorListener: DetectorListener,
    private val message: (String) -> Unit
) {

    private var interpreter: Interpreter
    var labels = mutableListOf<String>()

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0

    // Settings for thresholds
    val settings = DetectionSettings()

    // FPS calculation variables
    private var frameCount = 0
    private var lastFpsTime = SystemClock.uptimeMillis()
    private var currentFps = 0f
    private val fpsUpdateInterval = 1000L // Update FPS every 1 second

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    init {
        val compatList = CompatibilityList()

        val options = Interpreter.Options().apply{
            if(compatList.isDelegateSupportedOnThisDevice){
                val delegateOptions = compatList.bestOptionsForThisDevice
                this.addDelegate(GpuDelegate(delegateOptions))
            } else {
                this.setNumThreads(4)
            }
        }

        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model, options)

        val inputShape = interpreter.getInputTensor(0)?.shape()
        val outputShape = interpreter.getOutputTensor(0)?.shape()

        // Load labels from metadata first
        labels.addAll(extractNamesFromMetadata(model))
        if (labels.isEmpty()) {
            if (labelPath == null) {
                Log.w("Detector", "Model doesn't contain metadata, using default classes")
                message("Model doesn't contain metadata, using default classes")
                labels.addAll(MetaData.TEMP_CLASSES)
            } else {
                labels.addAll(extractNamesFromLabelFile(context, labelPath))
            }
        }

        Log.d("Detector", "Loaded ${labels.size} classes from model: $modelPath")
        message("Loaded model: $modelPath with ${labels.size} classes")

        if (inputShape != null) {
            tensorWidth = inputShape[1]
            tensorHeight = inputShape[2]

            // If in case input shape is in format of [1, 3, ..., ...]
            if (inputShape[1] == 3) {
                tensorWidth = inputShape[2]
                tensorHeight = inputShape[3]
            }
        }

        if (outputShape != null) {
            numChannel = outputShape[1]
            numElements = outputShape[2]
        }

        Log.d("Detector", "Model input shape: ${inputShape?.contentToString()}")
        Log.d("Detector", "Model output shape: ${outputShape?.contentToString()}")
        Log.d("Detector", "Tensor dimensions: ${tensorWidth}x${tensorHeight}")
    }

    // Add this method to the Detector class
    fun updateSettings(confidenceThreshold: Float, iouThreshold: Float, boxTransparency: Float) {
        settings.confidenceThreshold = confidenceThreshold.coerceIn(0.1f, 0.9f)
        settings.iouThreshold = iouThreshold.coerceIn(0.1f, 0.9f)
        settings.boxTransparency = boxTransparency.coerceIn(0.1f, 1.0f)
        Log.d("Detector", "Updated settings - Conf: ${settings.confidenceThreshold}, IoU: ${settings.iouThreshold}, Transparency: ${settings.boxTransparency}")
    }

    // Keep the old method for backward compatibility
    fun updateSettings(confidenceThreshold: Float, iouThreshold: Float) {
        updateSettings(confidenceThreshold, iouThreshold, settings.boxTransparency)
    }

    fun restart(isGpu: Boolean) {
        interpreter.close()

        val options = if (isGpu) {
            val compatList = CompatibilityList()
            Interpreter.Options().apply{
                if(compatList.isDelegateSupportedOnThisDevice){
                    val delegateOptions = compatList.bestOptionsForThisDevice
                    this.addDelegate(GpuDelegate(delegateOptions))
                } else {
                    this.setNumThreads(4)
                }
            }
        } else {
            Interpreter.Options().apply{
                this.setNumThreads(4)
            }
        }

        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model, options)

        // Reset FPS counter when switching modes
        resetFpsCounter()

        message("Switched to ${if (isGpu) "GPU" else "CPU"} mode")
    }

    fun close() {
        interpreter.close()
    }

    private fun resetFpsCounter() {
        frameCount = 0
        lastFpsTime = SystemClock.uptimeMillis()
        currentFps = 0f
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

    fun detect(frame: Bitmap) {
        if (tensorWidth == 0
            || tensorHeight == 0
            || numChannel == 0
            || numElements == 0) {
            return
        }

        val totalStartTime = SystemClock.uptimeMillis()

        // Preprocessing time
        val preprocessStartTime = SystemClock.uptimeMillis()
        val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)

        val tensorImage = TensorImage(INPUT_IMAGE_TYPE)
        tensorImage.load(resizedBitmap)
        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer
        val preprocessingTime = SystemClock.uptimeMillis() - preprocessStartTime

        // Inference time
        val inferenceStartTime = SystemClock.uptimeMillis()
        val output = TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter.run(imageBuffer, output.buffer)
        val inferenceTime = SystemClock.uptimeMillis() - inferenceStartTime

        // Postprocessing time
        val postprocessStartTime = SystemClock.uptimeMillis()
        val bestBoxes = bestBox(output.floatArray)
        val postprocessingTime = SystemClock.uptimeMillis() - postprocessStartTime

        val totalTime = SystemClock.uptimeMillis() - totalStartTime
        val fps = calculateFps()

        // Calculate class counts
        val classCounts = if (bestBoxes != null) {
            bestBoxes.groupingBy { it.clsName }.eachCount()
        } else {
            emptyMap()
        }

        val metrics = DetectionMetrics(
            inferenceTime = inferenceTime,
            preprocessingTime = preprocessingTime,
            postprocessingTime = postprocessingTime,
            totalTime = totalTime,
            fps = fps,
            classCounts = classCounts
        )

        if (bestBoxes == null) {
            detectorListener.onEmptyDetect(metrics)
            return
        }

        detectorListener.onDetect(bestBoxes, metrics)
    }

    private fun bestBox(array: FloatArray) : List<BoundingBox>? {

        val boundingBoxes = mutableListOf<BoundingBox>()

        for (c in 0 until numElements) {
            var maxConf = settings.confidenceThreshold // Use configurable threshold
            var maxIdx = -1
            var j = 4
            var arrayIdx = c + numElements * j
            while (j < numChannel){
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            if (maxConf > settings.confidenceThreshold) {
                // Ensure we don't go out of bounds
                val classIndex = maxIdx.coerceIn(0, labels.size - 1)
                val clsName = if (labels.isNotEmpty()) labels[classIndex] else "Unknown"

                val cx = array[c] // 0
                val cy = array[c + numElements] // 1
                val w = array[c + numElements * 2]
                val h = array[c + numElements * 3]
                val x1 = cx - (w/2F)
                val y1 = cy - (h/2F)
                val x2 = cx + (w/2F)
                val y2 = cy + (h/2F)

                if (x1 < 0F || x1 > 1F) continue
                if (y1 < 0F || y1 > 1F) continue
                if (x2 < 0F || x2 > 1F) continue
                if (y2 < 0F || y2 > 1F) continue

                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        cx = cx, cy = cy, w = w, h = h,
                        cnf = maxConf, cls = classIndex, clsName = clsName
                    )
                )
            }
        }

        if (boundingBoxes.isEmpty()) return null

        return applyNMS(boundingBoxes)
    }

    private fun applyNMS(boxes: List<BoundingBox>) : MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while(sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= settings.iouThreshold) { // Use configurable IoU threshold
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