package ca.stewark.nocturnel.ui.playback.visualizer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
internal fun ImmersiveSystemBars() {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity) {
        val decor = activity.window.decorView
        val insets = ViewCompat.getRootWindowInsets(decor)
        val statusWasVisible = insets?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: true
        val navigationWasVisible = insets?.isVisible(WindowInsetsCompat.Type.navigationBars()) ?: true
        val captionWasVisible = insets?.isVisible(WindowInsetsCompat.Type.captionBar()) ?: false
        val controller = WindowCompat.getInsetsController(activity.window, decor)
        val priorBehavior = controller.systemBarsBehavior
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller.systemBarsBehavior = priorBehavior
            if (statusWasVisible) controller.show(WindowInsetsCompat.Type.statusBars())
            else controller.hide(WindowInsetsCompat.Type.statusBars())
            if (navigationWasVisible) controller.show(WindowInsetsCompat.Type.navigationBars())
            else controller.hide(WindowInsetsCompat.Type.navigationBars())
            if (captionWasVisible) controller.show(WindowInsetsCompat.Type.captionBar())
            else controller.hide(WindowInsetsCompat.Type.captionBar())
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
