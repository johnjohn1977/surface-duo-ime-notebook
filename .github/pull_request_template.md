<!--
SPDX-License-Identifier: LGPL-2.1-or-later
SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
-->

## Summary

Describe the user-visible change and why it is needed.

## Verification

- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew :app:assembleDebug :plugin:chewing:assembleDebug`
- [ ] `./gradlew :app:lintDebug`
- [ ] Device-tested on Surface Duo generation 1, or marked not applicable below

Device test notes / reason not applicable:

## Safety and privacy

- [ ] No root, Shizuku, hidden API, ROM, partition, or shell-daemon dependency was added
- [ ] IME and Accessibility consent remain user-controlled in Android Settings
- [ ] Accessibility does not read window content or capture typed text
- [ ] Pointer and gesture coordinates remain inside the resolved work panel
- [ ] No APK, signing material, credentials, serial numbers, device snapshots, or private logs are included

## Screenshots

Add screenshots for layout or interaction changes when useful.
