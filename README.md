<!--
SPDX-License-Identifier: LGPL-2.1-or-later
SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
-->

# Surface Duo IME Notebook

[![Android CI](https://github.com/johnjohn1977/surface-duo-ime-notebook/actions/workflows/android-ci.yml/badge.svg)](https://github.com/johnjohn1977/surface-duo-ime-notebook/actions/workflows/android-ci.yml)

An experimental, non-root Android notebook keyboard and touchpad for the
first-generation Microsoft Surface Duo.

The project turns the lower display into a notebook-style input deck while an
app remains focused on the upper display. The same keyboard can also appear as
the normal Android IME when the target app is full-screen.

This is a product fork of
[Fcitx5 for Android](https://github.com/fcitx5-android/fcitx5-android), pinned
to upstream version `0.1.3` at commit
`048f581c652367567b8ee5c28c5163b805288895`.

## Current features

- Six-row notebook keyboard with function keys, modifiers and full-size arrow
  keys.
- Lower-display touchpad with bounded pointer movement, tap, long-press,
  drag mode and a thumb-operated scroll strip.
- English and Traditional Chinese Zhuyin input in the same keyboard surface.
- Chewing candidate selection through the matching Fcitx plugin APK.
- One visible keyboard design in both split-screen and full-screen app modes.
- Surface Duo hinge geometry through Jetpack WindowManager, with a safe
  aspect-ratio fallback.
- No root, Shizuku, hidden Android APIs, ROM changes or background shell
  daemon.

## Android integration and privacy

The app uses two Android user-consent mechanisms:

1. **Input method:** owns keyboard input so the target editor keeps focus.
2. **Accessibility service:** dispatches bounded pointer gestures only inside
   the resolved work panel.

The accessibility service does not retrieve window content, capture typed
text, inspect the accessibility tree or make its overlay touchable. Android
Settings remains the only place where the user can enable the IME and
Accessibility service.

## Build

Clone recursively because the Fcitx engines are pinned Git submodules:

```bash
git clone --recurse-submodules \
  https://github.com/johnjohn1977/surface-duo-ime-notebook.git
cd surface-duo-ime-notebook
```

The upstream project requires Android SDK/Build Tools, NDK, CMake,
KDE extra-cmake-modules and GNU gettext. The exact Android and native tool
versions are defined by the checked-in Gradle convention files.

Run the host verification tasks:

```bash
./gradlew :app:testDebugUnitTest
./gradlew \
  :app:assembleDebug \
  :plugin:chewing:assembleDebug \
  :app:lintDebug
```

The app and Chewing APKs are written below their respective
`build/outputs/apk/debug/` directories. APKs and local signing material are
intentionally excluded from source control.

## Install for development

Install both debug APKs with ADB, then manually select **Duo Deck (Fcitx)** in
Android's input-method picker and manually enable **Duo Deck Notebook** under
Accessibility settings.

```bash
adb install -r app/build/outputs/apk/debug/*.apk
adb install -r plugin/chewing/build/outputs/apk/debug/*.apk
```

Do not automate either Android consent screen with ADB.

## Project status

The current prototype is device-tested on Surface Duo generation 1. It is not
a production release and has no compatibility claim for Surface Duo 2 or
ordinary single-screen phones.

See [UPSTREAM.md](UPSTREAM.md) for provenance and local baseline fixes.
Development and review expectations are documented in
[CONTRIBUTING.md](CONTRIBUTING.md). Please report sensitive problems through
the process in [SECURITY.md](SECURITY.md), not a public issue.

## License

This fork retains the upstream project license and third-party notices. See
[LICENSE](LICENSE) and the licenses in the pinned submodules.
