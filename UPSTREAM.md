<!--
SPDX-License-Identifier: LGPL-2.1-or-later
SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
-->

# Upstream baseline

This is the project-owned working fork for Duo Deck Chinese input.

- Upstream: `https://github.com/fcitx5-android/fcitx5-android.git`
- Tag: `0.1.3`
- Commit: `048f581c652367567b8ee5c28c5163b805288895`
- Submodules: the recursive revisions recorded by that commit
- Immutable mirror: `../../sources/fcitx5-android/`

The first local build must succeed before any product code is changed. Keep
the existing `../duo-deck/` implementation as the English interaction and
touchpad reference while this fork gains the Duo-specific frontend.

The pinned tag has two host-build defects fixed locally before product work:

1. its theme serializer declares schema 2.1, but `ThemeSerializationTest`
   still expected a 2.0 document not to migrate;
2. its generated data descriptor writes into the lint input directory without
   declaring the Gradle 9.6 task dependency.
3. current lint reports hundreds of findings in the untouched tag. They are
   snapshotted in `app/lint-baseline.xml`; new Duo Deck findings still fail the
   build. Regenerate deliberately with
   `scripts/build-duo-deck-fcitx.sh --update-lint-baseline` from the workspace
   root after reviewing upstream changes.

Both fixes are build/test-only and do not change the runtime baseline.

The Duo Deck architecture, privacy boundary and build instructions are
summarized in [README.md](README.md).
