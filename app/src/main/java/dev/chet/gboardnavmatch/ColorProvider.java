package dev.chet.gboardnavmatch;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;

public class ColorProvider extends ContentProvider {
    static final String PREFS = "state";
    static final String KEY_COLOR = "color";
    static final String KEY_PACKAGE = "package";
    static final String KEY_TIME = "time";

    private SharedPreferences prefs() {
        return getContext().getSharedPreferences(PREFS, 0);
    }

    @Override public boolean onCreate() { return true; }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        Bundle out = new Bundle();
        if ("setColor".equals(method)) {
            int color = extras != null ? extras.getInt(KEY_COLOR, 0) : 0;
            String pkg = extras != null ? extras.getString(KEY_PACKAGE, "") : "";
            prefs().edit().putInt(KEY_COLOR, color).putString(KEY_PACKAGE, pkg)
                    .putLong(KEY_TIME, System.currentTimeMillis()).apply();
            out.putBoolean("ok", true);
            return out;
        }
        if ("getState".equals(method)) {
            out.putInt(KEY_COLOR, prefs().getInt(KEY_COLOR, 0));
            out.putString(KEY_PACKAGE, prefs().getString(KEY_PACKAGE, ""));
            out.putLong(KEY_TIME, prefs().getLong(KEY_TIME, 0L));
            out.putBoolean("enabled", prefs().getBoolean("enabled", true));
            out.putInt("adjust", prefs().getInt("adjust", 0));
            return out;
        }
        return super.call(method, arg, extras);
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        MatrixCursor c = new MatrixCursor(new String[]{KEY_COLOR, KEY_PACKAGE, KEY_TIME});
        c.addRow(new Object[]{prefs().getInt(KEY_COLOR, 0), prefs().getString(KEY_PACKAGE, ""), prefs().getLong(KEY_TIME, 0L)});
        return c;
    }
    @Override public String getType(Uri uri) { return "vnd.android.cursor.item/vnd.gboardnavmatch.color"; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
