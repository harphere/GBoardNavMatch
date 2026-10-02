package dev.chet.gboardnavmatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class StateReceiver extends BroadcastReceiver {
    public static final String ACTION_SET = "dev.chet.gboardnavmatch.SET_COLOR";
    public static final String ACTION_GET = "dev.chet.gboardnavmatch.GET_STATE";
    public static final String PREFS = "state";
    public static final String KEY_COLOR = "color";
    public static final String KEY_PACKAGE = "package";
    public static final String KEY_TIME = "time";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String action = intent.getAction();

        if (ACTION_SET.equals(action)) {
            int color = intent.getIntExtra(KEY_COLOR, 0);
            String pkg = intent.getStringExtra(KEY_PACKAGE);
            prefs.edit()
                    .putInt(KEY_COLOR, color)
                    .putString(KEY_PACKAGE, pkg == null ? "" : pkg)
                    .putLong(KEY_TIME, System.currentTimeMillis())
                    .apply();
            return;
        }

        if (ACTION_GET.equals(action)) {
            Bundle out = new Bundle();
            out.putInt(KEY_COLOR, prefs.getInt(KEY_COLOR, 0));
            out.putString(KEY_PACKAGE, prefs.getString(KEY_PACKAGE, ""));
            out.putLong(KEY_TIME, prefs.getLong(KEY_TIME, 0L));
            out.putBoolean("enabled", prefs.getBoolean("enabled", true));
            out.putInt("adjust", prefs.getInt("adjust", 0));
            setResultExtras(out);
        }
    }
}
