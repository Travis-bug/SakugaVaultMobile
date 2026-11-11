package ca.gbc.comp3074.sakugavault.api;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.ContextCompat;

import ca.gbc.comp3074.sakugavault.BuildConfig;

/**
 * Android 17 (API 37) blocks apps from connecting to addresses on the local network
 * unless they hold ACCESS_LOCAL_NETWORK. The local back end is on such an address
 * (10.0.2.2 from the emulator, or the computer's Wi-Fi IP from a phone), so without
 * this permission every API call just times out.
 */
public final class LocalNetworkAccess {

    public static final String PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK";
    private static final int ANDROID_17 = 37;

    private LocalNetworkAccess() {
    }

    /** True when this phone and this build need the permission to reach the API server. */
    public static boolean isNeeded(Context context) {
        return Build.VERSION.SDK_INT >= ANDROID_17
                && isLocalAddress(Uri.parse(BuildConfig.API_BASE_URL).getHost())
                && isDeclared(context);
    }

    public static boolean isGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    /** Private (LAN) addresses. Loopback (127.0.0.1, localhost) is not "local network" and needs no permission. */
    static boolean isLocalAddress(String host) {
        if (host == null) {
            return false;
        }
        if (host.endsWith(".local") || host.startsWith("10.") || host.startsWith("192.168.") || host.startsWith("169.254.")) {
            return true;
        }
        if (host.startsWith("172.")) {
            String[] parts = host.split("\\.");
            try {
                int second = Integer.parseInt(parts[1]);
                return second >= 16 && second <= 31;
            } catch (RuntimeException e) {
                return false;
            }
        }
        return false;
    }

    /** Only debug builds declare the permission (src/debug/AndroidManifest.xml). */
    private static boolean isDeclared(Context context) {
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions != null) {
                for (String permission : info.requestedPermissions) {
                    if (PERMISSION.equals(permission)) {
                        return true;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // Can't happen for our own package.
        }
        return false;
    }
}
