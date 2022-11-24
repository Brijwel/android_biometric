package com.brijwel.biometricexample

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private val executor: Executor by lazy { ContextCompat.getMainExecutor(this) }

    private val biometricManager: BiometricManager by lazy { BiometricManager.from(this) }

    private val promptInfo: BiometricPrompt.PromptInfo by lazy {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login")
            .setSubtitle("Log in using your biometric credential")
            .setAllowedAuthenticators(authenticators).build()
    }

    private val biometricPrompt: BiometricPrompt by lazy {
        BiometricPrompt(this, executor, biometricCallback)

    }
    private val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL


    private val biometricCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            when (errorCode) {
                BiometricPrompt.ERROR_LOCKOUT ->
                    toast("Locked out due to too many attempts. This occurs after 5 failed attempts, and lasts for 30 seconds.")
                BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                    toast("Biometric authentication is disabled until the user unlocks with their device credential.")
                BiometricPrompt.ERROR_NEGATIVE_BUTTON -> toast("Biometric cancelled by user.")
                BiometricPrompt.ERROR_TIMEOUT -> toast("Biometric authentication timeout.")
                BiometricPrompt.ERROR_USER_CANCELED -> toast("Biometric cancelled by user.")
                else -> toast("something went wrong.")
            }
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            toast("Biometric authentication success")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<AppCompatImageView>(R.id.authentication).apply {
            setOnClickListener {
                authenticate()
            }
        }
        updateBioMetricStatus()
    }

    private fun authenticate() {
        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> biometricPrompt.authenticate(promptInfo)
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> toast("No biometric features available on this device.")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> toast("Biometric features are currently unavailable.")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                toast("Biometric feature available but not enrolled.")
                // Prompts the user to create credentials that your app accepts.
                biometricEnrollmentRequest.launch(getFingerPrintEnrollmentIntent())
            }
            else -> toast("something went wrong.")
        }

    }

    private val biometricEnrollmentRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updateBioMetricStatus()
        }

    private fun updateBioMetricStatus() {
        findViewById<AppCompatTextView>(R.id.bioMetricStatus).text = getBioMetricStatus()
    }

    private fun getBioMetricStatus(): String {
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Hardware Unavailable"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Biometric None Enrolled"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No Biometric Hardware"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Security Update Required"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "Un Supported"
            BiometricManager.BIOMETRIC_SUCCESS -> "Available"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "Unknown"
            else -> "Unknown"
        }
    }

    private fun getFingerPrintEnrollmentIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, authenticators
                )
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                //Deprecated in API level 30
                @Suppress("DEPRECATION")
                Intent(Settings.ACTION_FINGERPRINT_ENROLL)
            } else {
                Intent(Settings.ACTION_SECURITY_SETTINGS)
            }
        }
    }
}


private fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}