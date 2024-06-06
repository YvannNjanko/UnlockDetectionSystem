package com.example.wizeman

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraService : Service() {

    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var imageReader: ImageReader
    private lateinit var captureRequestBuilder: CaptureRequest.Builder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        startCamera()
        return START_NOT_STICKY
    }

    private fun startCamera() {
        try {
            val cameraId = getFrontFacingCameraId()
            if (cameraId != null) {
                if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    stopSelf()
                    return
                }
                cameraManager.openCamera(cameraId, stateCallback, null)
            } else {
                Log.e(TAG, "Pas de camera frontale trouvee")
                stopSelf()
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun getFrontFacingCameraId(): String? {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return null
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
            stopSelf()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            stopSelf()
        }
    }

    private fun createCaptureSession() {
        try {
            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)
            imageReader.setOnImageAvailableListener(onImageAvailableListener, null)

            val surface = imageReader.surface
            captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder.addTarget(surface)

            cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    captureImage()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    stopSelf()
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun captureImage() {
        try {
            captureSession?.capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    Log.d(TAG, "Capture effectuee")
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        image?.let {
            val buffer: ByteBuffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)
            saveImage(bytes)
            image.close()
        } ?: run {
            Log.e(TAG, "Unable to acquire image")
        }
    }

    private fun saveImage(imageBytes: ByteArray) {
        try {
            val fileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (storageDir != null && storageDir.exists()) {
                val imageFile = File(storageDir, fileName)
                FileOutputStream(imageFile).use { fos ->
                    fos.write(imageBytes)
                }
                MediaScannerConnection.scanFile(this, arrayOf(imageFile.absolutePath), null, null)
                Log.d(TAG, "Image Sauvegardee: ${imageFile.absolutePath}")

                // Envoyer l'image par email
                sendImageByEmail(imageFile.absolutePath)

            } else {
                Log.e(TAG, "Dossier de Stockage impossible a trouver/acceder")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendImageByEmail(imagePath: String) {
        val email = "yvannjanko04@gmail.com"
        val subject = "Image Capturée"
        val body = "Voici l'image capturée après un échec de déverrouillage."

        // Initialiser le MailSender avec votre adresse email et le mot de passe d'application
        val mailSender = MailSender("yvannjanko04@gmail.com", "rvly uhkw rxnu snmv")

        // Envoyer l'email dans un coroutine
        CoroutineScope(Dispatchers.IO).launch {
            mailSender.sendMail(subject, body, listOf(email), imagePath)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val TAG = "CameraService"
    }
}
