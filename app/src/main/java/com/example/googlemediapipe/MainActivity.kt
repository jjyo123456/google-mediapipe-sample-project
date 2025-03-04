package com.example.googlemediapipe

import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private lateinit var textureView: TextureView
    private lateinit var handLandmarker: HandLandmarker
    private lateinit var cameraExecutor: ExecutorService
    private val path = Path()
    private var lastX = -1f
    private var lastY = -1f

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        surfaceView = findViewById(R.id.surfaceView)
        textureView = findViewById(R.id.textureView)

        setupHandLandmarker()
        setupDrawingCanvas()
        startCamera()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupHandLandmarker() {
        val baseOptions = BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(1)
            .setMinHandDetectionConfidence(0.5f)
            .setMinHandPresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setResultListener { result, _ ->
                runOnUiThread {
                    draw(result)
                }
            }
            .build()

        handLandmarker = HandLandmarker.createFromOptions(this, options)
    }

    private fun setupDrawingCanvas() {
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                val canvas = holder.lockCanvas()
                canvas?.drawColor(Color.WHITE)
                holder.unlockCanvasAndPost(canvas)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        })
    }

    // Camera setup
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(textureView.surfaceTextureListener)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processFrame(bitmap: Bitmap) {
        val mediaPipeImage = BitmapImageBuilder(bitmap).build()
        val timestampMs = System.currentTimeMillis()
        handLandmarker.detectAsync(mediaPipeImage, timestampMs) // Async detection for LIVE_STREAM
    }

    private fun draw(results: HandLandmarkerResult) {
        val holder = surfaceView.holder
        if (!holder.surface.isValid) return // Prevent crash if surface is not ready

        val canvas = holder.lockCanvas() ?: return
        canvas.drawColor(Color.WHITE)

        if (results.landmarks().isNotEmpty()) {
            val landmark = results.landmarks()[0][8] // Index finger tip

            val x = landmark.x() * surfaceView.width
            val y = landmark.y() * surfaceView.height

            if (lastX != -1f && lastY != -1f) {
                path.lineTo(x, y)
            } else {
                path.moveTo(x, y) // Move only for the first detected point
            }

            lastX = x
            lastY = y
        }

        canvas.drawPath(path, paint)
        holder.unlockCanvasAndPost(canvas)
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap() ?: return
        processFrame(bitmap)
        imageProxy.close()
    }

    private fun ImageProxy.toBitmap(): Bitmap? {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown() // Prevent memory leaks
    }
}
