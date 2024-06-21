package com.example.wizeman

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.location.Location
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

class CameraService : Service() {

    private lateinit var cameraManager: CameraManager
    private lateinit var locationService: LocationService
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var imageReader: ImageReader
    private lateinit var captureRequestBuilder: CaptureRequest.Builder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        locationService = LocationService(this)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        startForegroundService()
        startCamera()
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("my_service", "My Background Service")
        } else {
            ""
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Camera Service")
            .setContentText("Capturing image after failed unlock attempts.")
            .setSmallIcon(R.drawable.ic_notification) // Replace with your app's notification icon
            .build()

        startForeground(1, notification)
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
            chan.lightColor = android.graphics.Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)
        }
        return channelId
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
                Log.e(TAG, "No front-facing camera found")
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
                    Log.d(TAG, "Image captured")
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
                Log.d(TAG, "Image saved: ${imageFile.absolutePath}")

                // Send the image by email
                sendImageByEmail(imageFile.absolutePath, imageBytes)

            } else {
                Log.e(TAG, "Unable to access storage directory")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // send mail
    private fun sendImageByEmail(imagePath: String, imageBytes: ByteArray) {
        CoroutineScope(Dispatchers.Main).launch {
            val location: Location? = locationService.getCurrentLocation()
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude
                val user = FirebaseAuth.getInstance().currentUser
                val email = user?.email ?: "obsliferluka@gmail.com" // Default email if user is not logged in
                val subject = "Captured Image"
                val body = "Here is the image captured after a failed unlock attempt."

                // Initialize the MailSender with your email and app-specific password
                val mailSender = MailSender("obsliferluka@gmail.com", "pkqq ccaz xybq slmv")

                // Send the email in a coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    mailSender.sendMail(subject, body, listOf(email), imageBytes, imagePath, latitude, longitude)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val TAG = "CameraService"
    }
}
