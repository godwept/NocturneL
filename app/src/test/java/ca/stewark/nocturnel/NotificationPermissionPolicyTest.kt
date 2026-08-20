package ca.stewark.nocturnel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionPolicyTest {
    @Test fun requestsPermissionOnlyOnAndroid13AndNewerWhenMissing() {
        assertFalse(NotificationPermissionPolicy.shouldRequest(31, alreadyGranted = false))
        assertFalse(NotificationPermissionPolicy.shouldRequest(32, alreadyGranted = false))
        assertTrue(NotificationPermissionPolicy.shouldRequest(33, alreadyGranted = false))
        assertTrue(NotificationPermissionPolicy.shouldRequest(36, alreadyGranted = false))
        assertFalse(NotificationPermissionPolicy.shouldRequest(36, alreadyGranted = true))
    }
}
