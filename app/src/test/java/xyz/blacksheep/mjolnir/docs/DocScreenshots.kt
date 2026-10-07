package xyz.blacksheep.mjolnir.docs

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.blacksheep.mjolnir.R
import xyz.blacksheep.mjolnir.home.Action
import xyz.blacksheep.mjolnir.home.actionDescription
import xyz.blacksheep.mjolnir.home.actionLabel
import xyz.blacksheep.mjolnir.home.orderedActions
import xyz.blacksheep.mjolnir.model.MainScreen
import xyz.blacksheep.mjolnir.onboarding.ActionIcon
import xyz.blacksheep.mjolnir.settings.GestureConfigStore
import xyz.blacksheep.mjolnir.settings.GesturePresetCardRow
import xyz.blacksheep.mjolnir.settings.GesturePresetEditorScreen
import xyz.blacksheep.mjolnir.settings.HomeLauncherSettingsMenu
import xyz.blacksheep.mjolnir.ui.theme.MjolnirTheme

/**
 * Renders real app screens into docs/images/ for the user guide.
 * Run: ./gradlew testDebugUnitTest --tests '*DocScreenshots*' -Proborazzi.test.record=true
 * (AYN Thor top screen: 1920x1080.)
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp-land-night-xhdpi", sdk = [34])
class DocScreenshots {
    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val out = System.getProperty("docs.images.dir") ?: "build/doc-images"
    private val topPkg = "org.es_de.frontend"
    private val bottomPkg = "ginlemon.flowerfree"

    @Before
    fun fakeLaunchers() {
        fakeHomeApp(topPkg, "ES-DE", 0xFFE8A33D.toInt())
        fakeHomeApp(bottomPkg, "Smart Launcher", 0xFF3D8BE8.toInt())
        // Robolectric cannot load the adaptive launcher icon; use its foreground layer (the Mjolnir hammer).
        shadowOf(context.packageManager).setApplicationIcon(context.packageName, context.getDrawable(R.drawable.ic_launcher_foreground))
    }

    private fun fakeHomeApp(pkg: String, label: String, color: Int) {
        val pm = shadowOf(context.packageManager)
        pm.setApplicationIcon(pkg, GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color); setSize(96, 96) })
        pm.installPackage(PackageInfo().apply {
            packageName = pkg
            applicationInfo = ApplicationInfo().apply { packageName = pkg; nonLocalizedLabel = label; name = label }
        })
        val component = ComponentName(pkg, "$pkg.Main")
        pm.addActivityIfNotPresent(component)
        pm.addIntentFilterForActivity(component, IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME); addCategory(Intent.CATEGORY_DEFAULT)
        })
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        compose.setContent { MjolnirTheme { Surface(color = MaterialTheme.colorScheme.background) { content() } } }
        compose.onRoot().captureRoboImage("$out/$name.png")
    }

    private val example = GestureConfigStore.GestureConfig(
        fileName = "my-thor.cfg", name = "My Thor",
        single = Action.BOTH_HOME, double = Action.BOTTOM_HOME_DEFAULT,
        triple = Action.APP_SWITCH, long = Action.FOCUS_AUTO, longPressDelayMs = 500
    )

    @Test fun presetEditor() = shot("preset-editor") {
        GesturePresetEditorScreen("Edit Gesture Preset", "My Thor", topPkg, bottomPkg, example, {}, {}, {}, {})
    }

    @Test fun actionList() = shot("action-list") {
        Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(16.dp)) {
            orderedActions().forEach { action ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ActionIcon(action = action, topApp = topPkg, bottomApp = bottomPkg, size = 24.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(actionLabel(action, "ES-DE", "Smart Launcher"), Modifier.width(200.dp), color = MaterialTheme.colorScheme.onSurface)
                    Text(actionDescription(action), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    @Test fun presetCards() = shot("preset-cards") {
        GestureConfigStore.ensureDefaults(context)
        GesturePresetCardRow(
            presets = GestureConfigStore.listConfigs(context) + example,
            activeFileName = example.fileName, topAppPackage = topPkg, bottomAppPackage = bottomPkg,
            onSelect = {}, onEdit = {}, onCopy = {}, onShare = {}, onRename = {}, onDelete = {}, onNew = {}
        )
    }

    @Test fun homeApps() = shot("home-apps") {
        HomeLauncherSettingsMenu(
            navController = rememberNavController(), topApp = topPkg, onTopAppChange = {},
            bottomApp = bottomPkg, onBottomAppChange = {}, showAllApps = false, onShowAllAppsChange = {},
            onSetDefaultHome = {}, onLaunchDualScreen = {}, mainScreen = MainScreen.TOP, onMainScreenChange = {}
        )
    }
}
