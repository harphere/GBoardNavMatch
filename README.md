# GboardNavMatch v1.0.4

Standalone LSPosed diagnostic module for Android 16 / Vector Legacy Bridge.

## Design
1. Scoped foreground apps publish their effective navigation-bar colour through an explicit broadcast to the module app.
2. `StateReceiver` persists the latest colour, source package and timestamp in the module's private preferences.
3. When Gboard starts/shows its input view, it requests the persisted state using an explicit ordered broadcast.
4. Gboard applies the returned colour to its IME navigation bar and candidate keyboard/root surfaces.

This avoids external ContentProvider discovery, which can fail under Android package-visibility rules when the injected code runs with Gboard's app identity.

## v1.0.4 changes
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
