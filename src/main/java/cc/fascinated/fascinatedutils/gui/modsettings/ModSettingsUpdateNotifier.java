package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.updater.UpdateRequiredEvent;
import meteordevelopment.orbit.EventHandler;

/**
 * Simple static notifier that records whether an update has been reported by the updater
 * so UI code can show a small in-shell notification.
 */
public final class ModSettingsUpdateNotifier {
    private static volatile boolean updateAvailable = false;
    private static volatile String latestVersion = null;

    @EventHandler
    public static void onUpdateRequired(UpdateRequiredEvent event) {
        updateAvailable = true;
        latestVersion = event.latestVersion();
    }

    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public static String latestVersion() {
        return latestVersion;
    }

    public static void clear() {
        updateAvailable = false;
        latestVersion = null;
    }
}
