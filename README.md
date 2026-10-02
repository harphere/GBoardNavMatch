# Gboard Nav Match v1.0.0

Diagnostic LSPosed module for Android 16 that attempts to match Gboard's keyboard surface to the current foreground app's navigation-bar colour.

## Architecture

1. In every **scoped normal app**, the module hooks `Activity.onResume()` and `Window.setNavigationBarColor(int)`.
2. The latest non-transparent colour is sent by Binder IPC to the module's exported `ColorProvider`.
3. In **Gboard**, the module hooks `InputMethodService.onWindowShown()` and `onStartInputView()`.
4. When the keyboard appears it queries the provider, sets the IME navigation bar to the same colour, and recolours likely keyboard surface/root containers.

The provider contains only a colour integer, source package name, timestamp, and module settings. It exposes no files or privileged operations.

## LSPosed scope

Required:
- `Gboard` (`com.google.android.inputmethod.latin`)

Also select **each app whose navigation-bar colour you want Gboard to match**. For the first test, select a few known apps such as AquaMail, eero, Costco, Join and WhatsApp rather than your entire app list.

Do **not** scope Android System or System UI for v1.0.0.

## First test

1. Build and install the APK.
2. Enable it in LSPosed/Vector.
3. Scope Gboard + 3-5 test apps.
4. Force-stop Gboard and the test apps, or reboot.
5. Open a test app, tap a text field, then inspect Vector logs for `GboardNavMatch`.

Useful lines:

- `publisher hooks installed package=...`
- `publish resume #FF...... pkg=...`
- `loaded in Gboard process=...`
- `candidate depth=... class=... id=...`
- `apply onWindowShown #FF...... from=... changed=N`

If `publish` is correct but `changed=0`, the Gboard hook fired but the view classifier needs tuning.
If `changed>0` but the visible keyboard is unchanged, send the `candidate` lines; they identify what Gboard is actually drawing on your version.

## Build locally

Requirements: JDK 17+, Android SDK Platform 36, Build Tools 35.0.0+, Gradle 8.9+.

```bash
gradle :app:assembleDebug
```

APK output:
`app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions

Push this project to GitHub and run **Build APK**. The workflow intentionally installs only `platform-tools`, `platforms;android-36`, and `build-tools;35.0.0`; it does not request the obsolete `tools` SDK package.

## v1.0 limitations

- Transparent nav bars are ignored rather than trying to resolve the composited colour underneath them.
- Gboard uses obfuscated/private rendering internals, so this build deliberately uses framework IME hooks plus view-tree diagnostics rather than hard-coded Gboard class names.
- Foreground icons/key labels are left untouched in v1.0.0; first we need to confirm which Gboard surface views are stable on your installed build.
