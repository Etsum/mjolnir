package xyz.blacksheep.mjolnir.home

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.net.Uri
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplayManager
import xyz.blacksheep.mjolnir.KEY_BOTTOM_APP
import xyz.blacksheep.mjolnir.SafetyNetActivity
import xyz.blacksheep.mjolnir.KEY_HIDE_TOP_APP_FROM_RECENTS
import xyz.blacksheep.mjolnir.KEY_MAIN_SCREEN
import xyz.blacksheep.mjolnir.KEY_SHOW_ALL_APPS
import xyz.blacksheep.mjolnir.KEY_TOP_APP
import xyz.blacksheep.mjolnir.KEY_TOP_BOTTOM_LAUNCH_DELAY_MS
import xyz.blacksheep.mjolnir.settings.settingsPrefs

/** Regressions for upstream #34/#35 (Home actions hit both screens), #38 (Main Screen focus) and #40 (Hide from Recents), #31 (SafetyNet dead end). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeActionLauncherTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val pm get() = shadowOf(app.packageManager)
    private var bottomDisplayId = -1

    @Before
    fun setUp() {
        bottomDisplayId = ShadowDisplayManager.addDisplay("w400dp-h350dp")
        pm.setApplicationIcon(app.packageName, ColorDrawable())
        // Slot apps are launcher-only (picked via "show all apps"), so the default home is unambiguous.
        installApp("es.top", home = false, launcher = true)
        installApp("comp.bottom", home = false, launcher = true)
        app.settingsPrefs().edit().putBoolean(KEY_SHOW_ALL_APPS, true)
            .putString(KEY_TOP_APP, "es.top").putString(KEY_BOTTOM_APP, "comp.bottom")
            .putInt(KEY_TOP_BOTTOM_LAUNCH_DELAY_MS, 0).apply()
    }

    private fun installApp(pkg: String, home: Boolean, launcher: Boolean) {
        pm.installPackage(PackageInfo().apply { packageName = pkg; applicationInfo = ApplicationInfo().apply { packageName = pkg } })
        pm.setApplicationIcon(pkg, ColorDrawable())
        val c = ComponentName(pkg, "$pkg.Main")
        pm.addActivityIfNotPresent(c)
        if (home) pm.addIntentFilterForActivity(c, IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME); addCategory(Intent.CATEGORY_DEFAULT) })
        if (launcher) pm.addIntentFilterForActivity(c, IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) })
    }

    /** Installs [pkg] as the only HOME app, so it resolves as the default home. */
    private fun setDefaultHome(pkg: String) = installApp(pkg, home = true, launcher = true)

    private fun started(): List<Pair<Intent, Int>> {
        shadowOf(Looper.getMainLooper()).idle()
        val out = mutableListOf<Pair<Intent, Int>>()
        while (true) {
            val r = shadowOf(app).nextStartedActivityForResult ?: break
            out += r.intent to (r.options?.getInt("android.activity.launchDisplayId", -1) ?: -1)
        }
        return out.reversed() // Robolectric hands back the most recent launch first
    }

    @Test
    fun bottomHomeOpensThirdPartyDefaultOnBottomOnly() {
        app.settingsPrefs().edit().putString(KEY_TOP_APP, "NOTHING").apply()
        setDefaultHome("nova.home")
        HomeActionLauncher(app).launchDefaultHomeOnBottom()
        val launches = started()
        assertEquals(1, launches.size)
        assertEquals("nova.home", launches[0].first.component?.packageName ?: launches[0].first.`package`)
        assertTrue("launcher entry, not system HOME", launches[0].first.hasCategory(Intent.CATEGORY_LAUNCHER))
        assertEquals(bottomDisplayId, launches[0].second)
    }

    @Test
    fun quickstepDefaultIsNeverStartedDirectly() {
        setDefaultHome("com.android.launcher3")
        HomeActionLauncher(app).launchDefaultHomeOnBottom()
        started().forEach { assertTrue("Quickstep must not use the direct launcher path", !it.first.hasCategory(Intent.CATEGORY_LAUNCHER)) }
    }

    @Test
    fun bothAutoLaunchesMainScreenLast() {
        app.settingsPrefs().edit().putString(KEY_MAIN_SCREEN, "TOP").apply()
        HomeActionLauncher(app).launchBoth()
        assertEquals(listOf("comp.bottom", "es.top"), started().map { it.first.component?.packageName ?: it.first.`package` })

        app.settingsPrefs().edit().putString(KEY_MAIN_SCREEN, "BOTTOM").apply()
        HomeActionLauncher(app).launchBoth()
        assertEquals(listOf("es.top", "comp.bottom"), started().map { it.first.component?.packageName ?: it.first.`package` })
    }

    @Test
    fun hideFromRecentsAppliesOnlyToItsSlot() {
        app.settingsPrefs().edit().putBoolean(KEY_HIDE_TOP_APP_FROM_RECENTS, true).apply()
        HomeActionLauncher(app).launchBoth()
        val hidden = started().associate {
            (it.first.component?.packageName ?: it.first.`package`) to (it.first.flags and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS != 0)
        }
        assertEquals(mapOf("es.top" to true, "comp.bottom" to false), hidden)
    }

    private fun pkgs() = started().map { it.first.component?.packageName ?: it.first.`package` }

    @Test
    fun reopenSlotAppSkipsEmptyAndSystemHomeSlots() {
        HomeActionLauncher(app).reopenSlotApp(isTop = false)
        assertEquals(listOf(bottomDisplayId), started().map { it.second })

        app.settingsPrefs().edit().putString(KEY_BOTTOM_APP, "NOTHING").putString(KEY_TOP_APP, "com.android.launcher3").apply()
        HomeActionLauncher(app).reopenSlotApp(isTop = false)
        HomeActionLauncher(app).reopenSlotApp(isTop = true)
        assertEquals(emptyList<String?>(), pkgs())
    }

    @Test
    fun uncoveredSafetyNetReopensSlotAppOnceThenStays() {
        fun safetyNet(displayId: Int) = Robolectric.buildActivity(SafetyNetActivity::class.java,
            Intent(app, SafetyNetActivity::class.java).setData(Uri.parse("mjolnir://safetynet/$displayId"))).setup()

        val bottom = safetyNet(bottomDisplayId)
        started() // drop launches from setup
        bottom.pause().stop().restart().start().resume()
        assertEquals(listOf("comp.bottom"), pkgs())
        bottom.pause().stop().restart().start().resume() // slot app closed again right away
        assertEquals(emptyList<String?>(), pkgs())

        val external = safetyNet(99)
        external.pause().stop().restart()
        assertTrue("SafetyNet on an external display closes itself", external.get().isFinishing)
        assertEquals(emptyList<String?>(), pkgs())
    }
}
