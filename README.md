# GboardNavMatch v1.0.9

Standalone LSPosed diagnostic module for Android 16 / Vector Legacy Bridge.

## Design
1. Scoped foreground apps publish their effective navigation-bar colour through an explicit broadcast to the module app.
2. `StateReceiver` persists the latest colour, source package and timestamp in the module's private preferences.
3. When Gboard starts/shows its input view, it requests the persisted state using an explicit ordered broadcast.
4. Gboard applies the returned colour to its IME navigation bar and candidate keyboard/root surfaces.

This avoids external ContentProvider discovery, which can fail under Android package-visibility rules when the injected code runs with Gboard's app identity.

## v1.0.9 changes
- Replaced the cross-app ContentProvider IPC path with an explicit broadcast bridge.
- Removed all `content://dev.chet.gboardnavmatch.colors` lookups.
- No longer attempts to hook abstract `android.view.Window#setNavigationBarColor`.
- Hooks the concrete runtime Window implementation discovered from each Activity.
- Adds `Activity.onWindowFocusChanged(true)` publishing as a second refresh path.
- Retains Java 17-compatible Xposed stubs and the GitHub workflow that avoids the obsolete Android SDK `tools` package.

## LSPosed scope
Select Gboard (`com.google.android.inputmethod.latin`) plus the apps whose navigation colours should be captured, for example AquaMail, eero, Costco, Join and WhatsApp.

## Useful Vector log lines
Expected publisher-side messages:

    GboardNavMatch: Activity.onResume hook installed package=...
    GboardNavMatch: publisher hooks installed package=...
    GboardNavMatch: concrete nav-colour hook installed class=... pkg=...
    GboardNavMatch: publish resume #FF...... pkg=...

Expected Gboard-side messages:

    GboardNavMatch: onWindowShown hook installed
    GboardNavMatch: onStartInputView hook installed
    GboardNavMatch: state request sent onStartInputView
    GboardNavMatch: apply onStartInputView #FF...... from=... age=...ms changed=N


## v1.0.9 targeting change
The IME DecorView is never recoloured. The module now scores bottom-anchored, keyboard-sized containers and prefers Gboard's `ShrinkableFrameView`, with `input_area` as a lower-priority diagnostic candidate. This prevents the full-screen colour wash seen in v1.0.4.


## v1.0.9 keyboard-surface targeting

- Never paints `ShrinkableFrameView`; it is treated as a structural wrapper only.
- Targets `SoftKeyboardView` for the key-field background.
- Only tints pre-existing backgrounds on `KeyboardHolder` and `KeyboardViewHolder`; it does not create opaque wrapper backgrounds.
- Re-applies after 180 ms because Gboard may finish rebinding its keyboard views after `onStartInputView`.
- Logs background drawable class, size, y-position, visibility and alpha for the key surfaces.

## v1.0.9 key-cap renderer experiment

v1.0.9 keeps the v1.0.6 SoftKeyboardView background matching and adds a scoped drawing hook for Gboard's rendered key caps. While SoftKeyboardView is drawing, rounded-rectangle Paint objects are copied and recoloured to the currently matched navigation-bar colour. The original Paint is never mutated, so labels/icons drawn afterward retain their own colours. Diagnostic logs begin with `keycap roundRect` and report the source and replacement colours.


## v1.0.9
- Fixes Vector legacy compatibility compile error by logging the reflected draw method instead of `MethodHookParam.method`.
- Applies the current app colour to the IME navigation bar, disables navigation-bar contrast enforcement, and updates light/dark navigation icon appearance.
- Adds IME navigation-bar diagnostics.


## v1.0.9

- Adds targeted tinting for the Gboard suggestion/toolbar band above `SoftKeyboardView`.
- Adds a Launcher3/Quickstep-side navigation-surface hook for 3-button navigation.
- Launcher3 is now included in the suggested/default LSPosed scope.
- Keeps the working v1.0.8 keycap renderer hook unchanged.

Expected new logs include `suggestion/toolbar tint ...`, `toolbar target ...`, and `Launcher3 nav surface ...`.
