package dev.chet.gboardnavmatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedInit implements IXposedHookLoadPackage {
    private static final String TAG = "GboardNavMatch";
    private static final String MODULE = "dev.chet.gboardnavmatch";
    private static final String RECEIVER = "dev.chet.gboardnavmatch.StateReceiver";
    private static final String GBOARD = "com.google.android.inputmethod.latin";
    private static volatile int lastPublished = Integer.MIN_VALUE;
    private static final Set<Class<?>> HOOKED_WINDOW_CLASSES =
            Collections.newSetFromMap(new IdentityHashMap<>());

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName == null) return;
        if (GBOARD.equals(lpparam.packageName)) {
            log("loaded in Gboard process=" + lpparam.processName);
            installGboardHooks();
        } else if (!MODULE.equals(lpparam.packageName) && !"android".equals(lpparam.packageName)) {
            installPublisherHooks(lpparam);
        }
    }

    private void installPublisherHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        boolean installedAny = false;

        try {
            Method onResume = Activity.class.getDeclaredMethod("onResume");
            onResume.setAccessible(true);
            XposedBridge.hookMethod(onResume, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Activity a = (Activity) param.thisObject;
                        Window w = a.getWindow();
                        ensureConcreteWindowHook(w, lpparam.packageName);
                        publish(a, w.getNavigationBarColor(), "resume");
                    } catch (Throwable t) {
                        log("onResume publish failed pkg=" + lpparam.packageName + " " + t);
                    }
                }
            });
            installedAny = true;
            log("Activity.onResume hook installed package=" + lpparam.packageName);
        } catch (Throwable t) {
            log("Activity.onResume hook failed package=" + lpparam.packageName + " " + t);
        }

        try {
            Method onWindowFocusChanged = Activity.class.getMethod("onWindowFocusChanged", boolean.class);
            XposedBridge.hookMethod(onWindowFocusChanged, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!Boolean.TRUE.equals(param.args[0])) return;
                    try {
                        Activity a = (Activity) param.thisObject;
                        Window w = a.getWindow();
                        ensureConcreteWindowHook(w, lpparam.packageName);
                        publish(a, w.getNavigationBarColor(), "focus");
                    } catch (Throwable t) {
                        log("focus publish failed pkg=" + lpparam.packageName + " " + t);
                    }
                }
            });
            installedAny = true;
            log("Activity.onWindowFocusChanged hook installed package=" + lpparam.packageName);
        } catch (Throwable t) {
            log("Activity.onWindowFocusChanged hook failed package=" + lpparam.packageName + " " + t);
        }

        if (installedAny) log("publisher hooks installed package=" + lpparam.packageName);
    }

    private void ensureConcreteWindowHook(Window window, String packageName) {
        if (window == null) return;
        Class<?> cls = window.getClass();
        synchronized (HOOKED_WINDOW_CLASSES) {
            if (HOOKED_WINDOW_CLASSES.contains(cls)) return;
            HOOKED_WINDOW_CLASSES.add(cls);
        }

        try {
            Method m = findConcreteMethod(cls, "setNavigationBarColor", int.class);
            if (m == null) {
                log("no concrete setNavigationBarColor found class=" + cls.getName() + " pkg=" + packageName);
                return;
            }
            m.setAccessible(true);
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Window w = (Window) param.thisObject;
                        publish(w.getContext(), (Integer) param.args[0], "setNavigationBarColor");
                    } catch (Throwable t) {
                        log("setNavigationBarColor publish failed pkg=" + packageName + " " + t);
                    }
                }
            });
            log("concrete nav-colour hook installed class=" + cls.getName() + " pkg=" + packageName);
        } catch (Throwable t) {
            log("concrete nav-colour hook failed class=" + cls.getName() + " pkg=" + packageName + " " + t);
        }
    }

    private Method findConcreteMethod(Class<?> start, String name, Class<?>... params) {
        Class<?> c = start;
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                if (!Modifier.isAbstract(m.getModifiers())) return m;
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable t) {
                log("method lookup failed class=" + c.getName() + " " + t);
                return null;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private void publish(Context c, int color, String why) {
        if (c == null || Color.alpha(color) == 0 || color == lastPublished) return;
        lastPublished = color;
        try {
            Intent i = new Intent(StateReceiver.ACTION_SET);
            i.setComponent(new ComponentName(MODULE, RECEIVER));
            i.putExtra(StateReceiver.KEY_COLOR, color);
            i.putExtra(StateReceiver.KEY_PACKAGE, c.getPackageName());
            c.sendBroadcast(i);
            log(String.format(Locale.US, "publish %s #%08X pkg=%s", why, color, c.getPackageName()));
        } catch (Throwable t) {
            log("broadcast publish failed pkg=" + c.getPackageName() + " " + t);
        }
    }

    private void installGboardHooks() {
        try {
            Method onWindowShown = InputMethodService.class.getMethod("onWindowShown");
            XposedBridge.hookMethod(onWindowShown, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    requestAndApply((InputMethodService) param.thisObject, "onWindowShown");
                }
            });
            log("onWindowShown hook installed");
        } catch (Throwable t) {
            log("onWindowShown hook failed " + t);
        }

        try {
            Method onStartInputView = InputMethodService.class.getMethod(
                    "onStartInputView", android.view.inputmethod.EditorInfo.class, boolean.class);
            XposedBridge.hookMethod(onStartInputView, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    requestAndApply((InputMethodService) param.thisObject, "onStartInputView");
                }
            });
            log("onStartInputView hook installed");
        } catch (Throwable t) {
            log("onStartInputView hook failed " + t);
        }
    }

    private void requestAndApply(InputMethodService ime, String why) {
        try {
            Intent i = new Intent(StateReceiver.ACTION_GET);
            i.setComponent(new ComponentName(MODULE, RECEIVER));
            ime.sendOrderedBroadcast(i, null, new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        Bundle state = getResultExtras(false);
                        applyState(ime, why, state);
                    } catch (Throwable t) {
                        log("state reply failed " + why + " " + t);
                    }
                }
            }, null, Activity.RESULT_OK, null, null);
            log("state request sent " + why);
        } catch (Throwable t) {
            log("state request failed " + why + " " + t);
        }
    }

    private void applyState(InputMethodService ime, String why, Bundle state) {
        try {
            if (state == null) {
                log("no state reply at " + why);
                return;
            }
            if (!state.getBoolean("enabled", true)) {
                log("disabled at " + why);
                return;
            }
            int color = state.getInt(StateReceiver.KEY_COLOR, 0);
            long time = state.getLong(StateReceiver.KEY_TIME, 0L);
            String pkg = state.getString(StateReceiver.KEY_PACKAGE, "?");
            int adjust = state.getInt("adjust", 0);
            if (color == 0 || Color.alpha(color) == 0) {
                log("no usable colour at " + why);
                return;
            }

            color = adjustBrightness(color, adjust);
            android.app.Dialog dialog = ime.getWindow();
            if (dialog == null || dialog.getWindow() == null) {
                log("IME window unavailable at " + why);
                return;
            }

            Window win = dialog.getWindow();
            win.setNavigationBarColor(color);
            View decor = win.getDecorView();
            if (decor == null) {
                log("IME decor unavailable at " + why);
                return;
            }

            View target = findKeyboardSurface(decor, decor.getWidth(), decor.getHeight());
            int changed = 0;
            if (target != null) {
                target.setBackgroundColor(color);
                changed = 1;
                log("keyboard target class=" + target.getClass().getName()
                        + " id=" + safeIdName(target)
                        + " size=" + target.getWidth() + "x" + target.getHeight()
                        + " y=" + viewY(target));
            } else {
                log("no keyboard surface candidate at " + why);
            }
            log(String.format(Locale.US,
                    "apply %s #%08X from=%s age=%dms changed=%d root=%dx%d",
                    why, color, pkg, Math.max(0, System.currentTimeMillis() - time),
                    changed, decor.getWidth(), decor.getHeight()));
        } catch (Throwable t) {
            log("apply failed " + why + " " + t);
        }
    }

    private View findKeyboardSurface(View root, int rootW, int rootH) {
        KeyboardCandidate best = new KeyboardCandidate();
        scanKeyboardCandidates(root, rootW, rootH, 0, best);
        return best.view;
    }

    private void scanKeyboardCandidates(View v, int rootW, int rootH, int depth, KeyboardCandidate best) {
        try {
            int w = v.getWidth();
            int h = v.getHeight();
            int y = viewY(v);
            int bottom = y + h;
            String idName = safeIdName(v);
            String cls = v.getClass().getName().toLowerCase(Locale.US);

            // Never paint the IME DecorView or another near-full-screen container.
            boolean fullScreenLike = depth == 0
                    || (rootH > 0 && h >= rootH * 0.70f)
                    || (rootW > 0 && rootH > 0 && w >= rootW * 0.95f && h >= rootH * 0.60f);

            boolean wide = rootW > 0 && w >= rootW * 0.88f;
            boolean keyboardHeight = rootH > 0 && h >= rootH * 0.20f && h <= rootH * 0.55f;
            boolean bottomAnchored = rootH > 0 && bottom >= rootH * 0.82f;
            boolean shrinkable = cls.contains("shrinkableframeview");
            boolean strongName = containsAny(idName, "keyboard", "input_area", "inputarea", "keyboard_holder", "keyboardholder")
                    || containsAny(cls, "keyboard");

            if (!fullScreenLike && wide && keyboardHeight && bottomAnchored) {
                int score = 0;
                if (shrinkable) score += 1000;
                if (strongName) score += 500;
                score += Math.min(400, h / 2);
                score += Math.min(200, w / 10);
                // Prefer a whole keyboard container over the smaller input_area child.
                if ("input_area".equals(idName)) score -= 150;

                log("candidate depth=" + depth + " class=" + v.getClass().getName()
                        + " id=" + idName + " size=" + w + "x" + h
                        + " y=" + y + " bottom=" + bottom + " score=" + score);

                if (score > best.score) {
                    best.score = score;
                    best.view = v;
                }
            }
        } catch (Throwable ignored) {
        }

        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                scanKeyboardCandidates(g.getChildAt(i), rootW, rootH, depth + 1, best);
            }
        }
    }

    private static String safeIdName(View v) {
        try {
            if (v.getId() != View.NO_ID) {
                return v.getResources().getResourceEntryName(v.getId()).toLowerCase(Locale.US);
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    private static int viewY(View v) {
        try {
            int[] loc = new int[2];
            v.getLocationInWindow(loc);
            return loc[1];
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static final class KeyboardCandidate {
        View view;
        int score = Integer.MIN_VALUE;
    }

    private static boolean containsAny(String s, String... terms) {
        if (s == null) return false;
        for (String t : terms) if (s.contains(t)) return true;
        return false;
    }

    private static int adjustBrightness(int color, int pct) {
        if (pct == 0) return color;
        float factor = 1f + pct / 100f;
        int r = clamp(Math.round(Color.red(color) * factor));
        int g = clamp(Math.round(Color.green(color) * factor));
        int b = clamp(Math.round(Color.blue(color) * factor));
        return Color.argb(Color.alpha(color), r, g, b);
    }

    private static int clamp(int x) {
        return Math.max(0, Math.min(255, x));
    }

    private static void log(String s) {
        XposedBridge.log(TAG + ": " + s);
    }
}
