package ca.stewark.nocturnel

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import ca.stewark.nocturnel.ui.NocturneLApp
import ca.stewark.nocturnel.ui.theme.NocturneLTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notificationPermissionGranted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (
            NotificationPermissionPolicy.shouldRequest(
                sdkInt = Build.VERSION.SDK_INT,
                alreadyGranted = notificationPermissionGranted,
            )
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            NocturneLTheme {
                NocturneLApp()
            }
        }
    }
}
