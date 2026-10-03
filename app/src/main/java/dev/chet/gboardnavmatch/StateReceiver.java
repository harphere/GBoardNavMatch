package dev.chet.gboardnavmatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class StateReceiver extends BroadcastReceiver {
    public static final String ACTION_SET = "dev.chet.gboardnavmatch.SET_COLOR";
    public static final String ACTION_GET = "dev.chet.gboardnavmatch.GET_STATE";
    public static final String ACTION_IME = "dev.chet.gboardnavmatch.IME_STATE";

    public static final String PREFS = "state";
    public static final String KEY_COLOR = "color";
    public static final String KEY_PACKAGE = "package";
    public static final String KEY_TIME = "time";
    public static final String KEY_REQUEST_PACKAGE = "request_package";
    public static final String KEY_IME_VISIBLE = "ime_visible";
    public static final String KEY_IME_PACKAGE = "ime_package";

    private static String colorKey(String pkg) {
        return "pkg_color_" + (pkg == null ? "" : pkg);
    }

    private static String timeKey(String pkg) {
        return "pkg_time_" + (pkg == null ? "" : pkg);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String action = intent.getAction();

        if (ACTION_SET.equals(action)) {
            int color = intent.getIntExtra(KEY_COLOR, 0);
            String pkg = intent.getStringExtra(KEY_PACKAGE);
            long now = System.currentTimeMillis();
            SharedPreferences.Editor e = prefs.edit()
                    .putInt(KEY_COLOR, color)
                    .putString(KEY_PACKAGE, pkg == null ? "" : pkg)
                    .putLong(KEY_TIME, now);
            if (pkg != null && !pkg.isEmpty()) {
                e.putInt(colorKey(pkg), color)
                 .putLong(timeKey(pkg), now);
            }
            e.apply();
            return;
        }

        if (ACTION_IME.equals(action)) {
            boolean visible = intent.getBooleanExtra(KEY_IME_VISIBLE, false);
            String pkg = intent.getStringExtra(KEY_IME_PACKAGE);
            prefs.edit()
                    .putBoolean(KEY_IME_VISIBLE, visible)
                    .putString(KEY_IME_PACKAGE, pkg == null ? "" : pkg)
                    .apply();
            return;
        }

        if (ACTION_GET.equals(action)) {
            String requested = intent.getStringExtra(KEY_REQUEST_PACKAGE);
            int color;
            long time;
            String pkg;

            if (requested != null && !requested.isEmpty()
                    && prefs.contains(colorKey(requested))) {
                color = prefs.getInt(colorKey(requested), 0);
                time = prefs.getLong(timeKey(requested), 0L);
                pkg = requested;
            } else {
                color = prefs.getInt(KEY_COLOR, 0);
                time = prefs.getLong(KEY_TIME, 0L);
                pkg = prefs.getString(KEY_PACKAGE, "");
            }

            Bundle out = new Bundle();
            out.putInt(KEY_COLOR, color);
            out.putString(KEY_PACKAGE, pkg);
            out.putLong(KEY_TIME, time);
            out.putBoolean("enabled", prefs.getBoolean("enabled", true));
            out.putInt("adjust", prefs.getInt("adjust", 0));
            out.putBoolean(KEY_IME_VISIBLE, prefs.getBoolean(KEY_IME_VISIBLE, false));
            out.putString(KEY_IME_PACKAGE, prefs.getString(KEY_IME_PACKAGE, ""));
            setResultExtras(out);
        }
    }
}
