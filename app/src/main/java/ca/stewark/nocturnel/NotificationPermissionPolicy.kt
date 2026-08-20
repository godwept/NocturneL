package ca.stewark.nocturnel

import android.os.Build

internal object NotificationPermissionPolicy {
    fun shouldRequest(sdkInt: Int, alreadyGranted: Boolean): Boolean =
        sdkInt >= Build.VERSION_CODES.TIRAMISU && !alreadyGranted
}
