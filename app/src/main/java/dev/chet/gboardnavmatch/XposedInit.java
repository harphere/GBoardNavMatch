package dev.chet.gboardnavmatch;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.lang.reflect.Method;
import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedInit implements IXposedHookLoadPackage {
    private static final String TAG = "GboardNavMatch";
    private static final String GBOARD = "com.google.android.inputmethod.latin";
    private static final Uri PROVIDER = Uri.parse("content://dev.chet.gboardnavmatch.colors");
    private static volatile int lastPublished = Integer.MIN_VALUE;

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName == null) return;
        if (GBOARD.equals(lpparam.packageName)) {
            log("loaded in Gboard process=" + lpparam.processName);
            installGboardHooks(lpparam);
        } else if (!lpparam.packageName.equals("dev.chet.gboardnavmatch") && !lpparam.packageName.equals("android")) {
            installPublisherHooks(lpparam);
        }
    }

    private void installPublisherHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Method onResume = Activity.class.getDeclaredMethod("onResume");
            onResume.setAccessible(true);
            XposedBridge.hookMethod(onResume, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Activity a = (Activity) param.thisObject;
                        publish(a, a.getWindow().getNavigationBarColor(), "resume");
                    } catch (Throwable t) { log("onResume publish failed " + t); }
                }
            });

            Method setNavigationBarColor = Window.class.getMethod("setNavigationBarColor", int.class);
            XposedBridge.hookMethod(setNavigationBarColor, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Window w = (Window) param.thisObject;
                        Context c = w.getContext();
                        publish(c, (Integer) param.args[0], "setNavigationBarColor");
                    } catch (Throwable t) { log("setNavigationBarColor publish failed " + t); }
                }
            });
            log("publisher hooks installed package=" + lpparam.packageName);
        } catch (Throwable t) {
            log("publisher hook install failed package=" + lpparam.packageName + " " + t);
        }
    }

    private void publish(Context c, int color, String why) {
        if (c == null || Color.alpha(color) == 0 || color == lastPublished) return;
        lastPublished = color;
        Bundle b = new Bundle();
        b.putInt("color", color);
        b.putString("package", c.getPackageName());
        try {
            c.getContentResolver().call(PROVIDER, "setColor", null, b);
            log(String.format(Locale.US, "publish %s #%08X pkg=%s", why, color, c.getPackageName()));
        } catch (Throwable t) {
            log("provider publish failed pkg=" + c.getPackageName() + " " + t);
        }
    }

    private void installGboardHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Method onWindowShown = InputMethodService.class.getMethod("onWindowShown");
            XposedBridge.hookMethod(onWindowShown, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) { applyToIme((InputMethodService) param.thisObject, "onWindowShown"); }
            });
            log("onWindowShown hook installed");
        } catch (Throwable t) { log("onWindowShown hook failed " + t); }

        try {
            Method onStartInputView = InputMethodService.class.getMethod("onStartInputView", android.view.inputmethod.EditorInfo.class, boolean.class);
            XposedBridge.hookMethod(onStartInputView, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) { applyToIme((InputMethodService) param.thisObject, "onStartInputView"); }
            });
            log("onStartInputView hook installed");
        } catch (Throwable t) { log("onStartInputView hook failed " + t); }
    }

    private void applyToIme(InputMethodService ime, String why) {
        try {
            Bundle state = ime.getContentResolver().call(PROVIDER, "getState", null, null);
            if (state == null || !state.getBoolean("enabled", true)) return;
            int color = state.getInt("color", 0);
            long time = state.getLong("time", 0L);
            String pkg = state.getString("package", "?");
            int adjust = state.getInt("adjust", 0);
            if (color == 0 || Color.alpha(color) == 0) {
                log("no usable colour at " + why);
                return;
            }
            color = adjustBrightness(color, adjust);
            android.app.Dialog dialog = ime.getWindow();
            if (dialog == null || dialog.getWindow() == null) return;
            Window win = dialog.getWindow();
            win.setNavigationBarColor(color);
            View decor = win.getDecorView();
            if (decor == null) return;
            int changed = recolorTree(decor, color, decor.getWidth(), decor.getHeight(), 0);
            log(String.format(Locale.US, "apply %s #%08X from=%s age=%dms changed=%d root=%dx%d", why, color, pkg, Math.max(0,System.currentTimeMillis()-time), changed, decor.getWidth(), decor.getHeight()));
        } catch (Throwable t) {
            log("apply failed " + why + " " + t);
        }
    }

    private int recolorTree(View v, int color, int rootW, int rootH, int depth) {
        int changed = 0;
        try {
            int w = v.getWidth(), h = v.getHeight();
            Drawable bg = v.getBackground();
            boolean huge = rootW > 0 && rootH > 0 && w >= rootW * 0.70f && h >= rootH * 0.35f;
            String idName = "";
            try { if (v.getId() != View.NO_ID) idName = v.getResources().getResourceEntryName(v.getId()).toLowerCase(Locale.US); } catch (Throwable ignored) {}
            String cls = v.getClass().getName().toLowerCase(Locale.US);
            boolean named = containsAny(idName, "keyboard","ime","input","body","root","container","background") || containsAny(cls, "keyboard","ime");
            if (depth == 0 || huge || named) {
                if (bg == null || bg instanceof ColorDrawable || depth == 0) {
                    v.setBackgroundColor(color);
                    changed++;
                    if (changed <= 12) log("candidate depth="+depth+" class="+v.getClass().getName()+" id="+idName+" size="+w+"x"+h+" huge="+huge+" named="+named);
                }
            }
        } catch (Throwable ignored) {}
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i=0;i<g.getChildCount();i++) changed += recolorTree(g.getChildAt(i), color, rootW, rootH, depth+1);
        }
        return changed;
    }

    private static boolean containsAny(String s, String... terms) {
        if (s == null) return false;
        for (String t: terms) if (s.contains(t)) return true;
        return false;
    }

    private static int adjustBrightness(int color, int pct) {
        if (pct == 0) return color;
        float factor = pct > 0 ? 1f + pct/100f : 1f + pct/100f;
        int r = clamp(Math.round(Color.red(color)*factor));
        int g = clamp(Math.round(Color.green(color)*factor));
        int b = clamp(Math.round(Color.blue(color)*factor));
        return Color.argb(Color.alpha(color), r,g,b);
    }
    private static int clamp(int x) { return Math.max(0, Math.min(255, x)); }
    private static void log(String s) { XposedBridge.log(TAG + ": " + s); }
}
