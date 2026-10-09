<!--
SPDX-License-Identifier: LGPL-2.1-or-later
SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
-->

# Contributing

Duo Deck is currently a focused Surface Duo generation 1 prototype. Please
open an issue before starting a large behavior or layout change so the scope
and device test plan are clear.

## Development setup

Clone the repository recursively and use the pinned Android SDK, Build Tools,
NDK and CMake versions declared in
`build-logic/convention/src/main/kotlin/Versions.kt`.

Before submitting a pull request, run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug :plugin:chewing:assembleDebug
./gradlew :app:lintDebug
```

Layout and gesture changes should also be tested on a first-generation Surface
Duo in the affected posture. Record whether the test covered notebook mode,
full-screen IME mode, or both.

## Project boundaries

- Keep the app non-root and limited to public Android APIs.
- Do not add Shizuku, hidden APIs, ROM changes, partition operations, or a
  background shell daemon.
- The IME must own keyboard and touchpad input so the target editor retains
  focus. Accessibility overlays must stay non-focusable and non-touchable.
- Accessibility may dispatch bounded gestures, but must not retrieve window
  content, inspect the accessibility tree, or capture typed text.
- Keep pointer and gesture coordinates inside the resolved work panel.
- Android Settings must remain the only place where users enable the IME and
  Accessibility service.

## Pull requests

Keep changes focused and describe the failure mode or user problem being
solved. Include screenshots for visible layout changes and add automated tests
where the behavior can be verified off-device. Never commit APKs, signing
material, credentials, device serial numbers, private logs, or device
snapshots.

The `main` branch is intended to stay buildable. Changes should enter through
a pull request after the `Android CI / Verify debug build` check passes.
