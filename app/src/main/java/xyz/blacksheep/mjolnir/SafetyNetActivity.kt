package xyz.blacksheep.mjolnir

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.Display
import android.util.Log
import android.app.ActivityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import xyz.blacksheep.mjolnir.home.HomeActionLauncher

class SafetyNetActivity : ComponentActivity() {
    companion object {
        private const val TAG = "SafetyNetActivity"
        /** If the slot app closes again within this time, stay here instead of a reopen loop. */
        private const val REOPEN_GUARD_MS = 5_000L
        internal val lastReopen = mutableMapOf<Int, Long>()

        /**
         * Call before an action moves a slot app to the other screen (Swap, FOCUS: <Top app>).
         * The move uncovers the SafetyNet, and a reopen would move the app straight back.
         */
        fun holdOffReopen(context: Context) {
            val now = SystemClock.elapsedRealtime()
            SafetyNetManager.builtInDisplayIds(context).forEach { lastReopen[it] = now }
        }
    }

    private val displayId get() = intent?.data?.lastPathSegment?.toIntOrNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        excludeTaskFromRecents()
        Log.d(TAG, "onCreate displayId=$displayId taskId=$taskId data=${intent?.data}")
        setContent { SafetyNetScreen() }
    }

    /** Called when the task above closed and uncovered this screen (upstream #31). */
    override fun onRestart() {
        super.onRestart()
        val id = displayId ?: return
        val builtIn = SafetyNetManager.builtInDisplayIds(this)
        if (id !in builtIn) {
            Log.d(TAG, "Uncovered on external displayId=$id, finishing")
            finish()
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (now - (lastReopen[id] ?: -REOPEN_GUARD_MS) < REOPEN_GUARD_MS) {
            Log.d(TAG, "Uncovered again on displayId=$id within guard, staying")
            return
        }
        lastReopen[id] = now
        HomeActionLauncher(this).reopenSlotApp(isTop = id == Display.DEFAULT_DISPLAY)
    }

    private fun excludeTaskFromRecents() {
        val activityManager = getSystemService(ActivityManager::class.java)
        val taskId = this.taskId
        activityManager.appTasks.firstOrNull { it.taskInfo.taskId == taskId }
            ?.setExcludeFromRecents(true)
    }
}

@Composable
private fun SafetyNetScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text("You should not be here.", color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}
