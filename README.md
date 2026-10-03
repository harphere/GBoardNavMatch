# GboardNavMatch v1.0.10

Standalone LSPosed module for Android 16 / Vector Legacy Bridge.

## v1.0.10 fixes

This build addresses the state/timing quirks seen in v1.0.9:

- Stores navigation colours **per source package** instead of relying only on one global last colour.
- Gboard requests the colour for the package in the current `EditorInfo`, so switching from Phone to AquaMail cannot reuse Phone's blue state.
- Clears the key-cap renderer colour immediately when the input package changes. Key caps will not render with a stale previous-app colour while the new state reply is in flight.
- Delayed Gboard re-apply is package-guarded and is skipped if focus has already moved to another app.
- Publishes explicit IME visible/hidden state from `onWindowShown`, `onStartInputView`, `onWindowHidden`, and `onFinishInputView`.
- Launcher3/Quickstep only tints the 3-button navigation surface while the IME is visible, and restores its original background when the keyboard closes.
- Adds a narrow bottom-strip drawable for the small gap between Gboard's `KeyboardHolder` and `ShrinkableFrameView`, removing the dark separator above the 3-button bar without painting the whole wrapper.
- Keeps the working SoftKeyboardView background, key-cap renderer, suggestion/toolbar tint and IME nav-bar handling from v1.0.9.

## LSPosed scope

Enable at least:

- Gboard (`com.google.android.inputmethod.latin`)
- Launcher3 (`com.android.launcher3`)
- each app whose nav colour you want captured (AquaMail, Phone, etc.)

## Useful new logs

    GboardNavMatch: input package changed -> ...; cleared stale render colour
    GboardNavMatch: IME visible=true pkg=... reason=...
    GboardNavMatch: IME visible=false pkg=... reason=...
    GboardNavMatch: delayed apply skipped stale pkg=... current=...
    GboardNavMatch: Launcher3 nav surface restored ...
    GboardNavMatch: bottom keyboard gap tint height=...

## First install note

Because v1.0.9 may already have painted the live Launcher3 navigation container, restart Launcher3/SystemUI or reboot once after installing v1.0.10. That gives v1.0.10 a clean original background to preserve and restore.
