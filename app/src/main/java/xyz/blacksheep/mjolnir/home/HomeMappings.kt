package xyz.blacksheep.mjolnir.home

/**
 * Returns a human-readable label for a given [Action].
 *
 * @param topAppLabel Optional label for the configured Top app.
 * @param bottomAppLabel Optional label for the configured Bottom app.
 */
fun actionLabel(action: Action, topAppLabel: String? = null, bottomAppLabel: String? = null): String {
    val topName = topAppLabel ?: "Top app"
    val bottomName = bottomAppLabel ?: "Bottom app"
    return when (action) {
        Action.NONE -> "<DO NOTHING>"
        Action.APP_SWITCH -> "TOP: Recent Tasks"
        Action.DEFAULT_HOME -> "FOCUS: Home"
        Action.TOP_HOME_DEFAULT -> "TOP: Home"
        Action.BOTTOM_HOME_DEFAULT -> "BOTTOM: Home"
        Action.BOTH_HOME_DEFAULT -> "BOTH: Home"
        Action.TOP_HOME -> "TOP: $topName"
        Action.BOTTOM_HOME -> "BOTTOM: $bottomName"
        Action.BOTH_HOME -> "BOTH: Auto"
        Action.FOCUS_AUTO -> "FOCUS: Auto"
        Action.FOCUS_TOP_APP -> "FOCUS: $topName"
    }
}

fun orderedActions(): List<Action> = listOf(
    Action.TOP_HOME,
    Action.BOTTOM_HOME,
    Action.BOTH_HOME,
    Action.FOCUS_AUTO,
    Action.TOP_HOME_DEFAULT,
    Action.BOTTOM_HOME_DEFAULT,
    Action.BOTH_HOME_DEFAULT,
    Action.DEFAULT_HOME,
    Action.APP_SWITCH,
    Action.FOCUS_TOP_APP,
    Action.NONE
)

/**
 * Returns a human-readable label for a given [Gesture].
 */
fun gestureLabel(gesture: Gesture): String {
    return when (gesture) {
        Gesture.SINGLE_HOME -> "Single press Home"
        Gesture.DOUBLE_HOME -> "Double-tap Home"
        Gesture.TRIPLE_HOME -> "Triple-tap Home"
        Gesture.LONG_HOME -> "Long-press Home"
    }
}

/**
 * One-line help text for an [Action], shown in the gesture picker (docs/USER-GUIDE.md uses the same words).
 */
fun actionDescription(action: Action): String = when (action) {
    Action.TOP_HOME -> "Opens your Top app on the top screen."
    Action.BOTTOM_HOME -> "Opens your Bottom app on the bottom screen."
    Action.BOTH_HOME -> "Opens your Top app on the top screen and your Bottom app on the bottom screen. The Main Screen gets the focus."
    Action.FOCUS_AUTO -> "Opens the Top app or the Bottom app, for the screen that has the focus."
    Action.TOP_HOME_DEFAULT -> "Opens your default home app on the top screen."
    Action.BOTTOM_HOME_DEFAULT -> "Opens your default home app on the bottom screen."
    Action.BOTH_HOME_DEFAULT -> "Sends the two screens to your default home app."
    Action.DEFAULT_HOME -> "Does the normal Android Home action. On the AYN Thor, this can change the two screens."
    Action.APP_SWITCH -> "Opens Recent Tasks."
    Action.FOCUS_TOP_APP -> "Opens your Top app on the screen that has the focus."
    Action.NONE -> "Does nothing."
}
