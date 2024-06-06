package com.example.wizeman

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    private val TAG = "MyDeviceAdminReceivers"

    private fun showToast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onEnabled(context: Context, intent: Intent) {
        Log.i(TAG, "onEnabled")
        showToast(context, "onEnabled")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "onDisableRequested"

    override fun onDisabled(context: Context, intent: Intent) {
        Log.i(TAG, "onDisabled")
        showToast(context, "onDisabled")
    }

    override fun onPasswordChanged(context: Context, intent: Intent, userHandle: UserHandle) =
        showToast(context, "onPasswordChanged")

    override fun onPasswordSucceeded(context: Context, intent: Intent, userHandle: UserHandle) {
        Log.i(TAG, "onPasswordSucceeded")
        showToast(context, "onPasswordSucceeded")
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        val sharedPref = context.getSharedPreferences("FAILED_ATTEMPTS", Context.MODE_PRIVATE)
        val failedAttempts = sharedPref.getInt("attempts", 0) + 1
        sharedPref.edit().putInt("attempts", failedAttempts).apply()

        if (failedAttempts == 1) {
            Log.w("superieur", "Mot de passe raté")
            sharedPref.edit().putInt("attempts", 0).apply()

            // Démarrer le service de la caméra
            val cameraServiceIntent = Intent(context, CameraService::class.java)
            ContextCompat.startForegroundService(context, cameraServiceIntent)
        }
    }
}
