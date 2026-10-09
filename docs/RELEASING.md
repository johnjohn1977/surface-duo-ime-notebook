<!--
SPDX-License-Identifier: LGPL-2.1-or-later
SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
-->

# Duo Deck release guide / 發版指南

## 繁體中文

正式 APK 必須使用固定且私密的 Android signing key。遺失 signing key 或密碼後，既有安裝將無法升級到使用不同金鑰簽署的新版；請把 keystore 與密碼分開離線備份，且絕對不要提交到 Git。

首次設定時，在可信任的離線環境執行：

```bash
keytool -genkeypair \
  -keystore duo-deck-release.jks \
  -alias duo-deck \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

接著在 GitHub repository 的 **Settings → Secrets and variables → Actions** 建立：

- `ANDROID_SIGN_KEY_BASE64`：`base64 -w 0 duo-deck-release.jks` 的完整輸出。
- `ANDROID_SIGN_KEY_PASSWORD`：keystore/key password。
- `ANDROID_SIGN_KEY_ALIAS`：上例為 `duo-deck`。

並建立 repository variable `ANDROID_SIGN_CERT_SHA256`，值為 signing certificate 的小寫 SHA-256 fingerprint（移除冒號）。Workflow 會比對主程式與 plugin 的實際簽章，避免誤用其他金鑰發布。

合併並確認 `main` CI 成功後，用 annotated tag 發版：

```bash
git switch main
git pull --ff-only
git tag -a v0.1.0-preview.1 -m 'Duo Deck 0.1.0 Preview 1'
git push origin v0.1.0-preview.1
```

`Android Release` workflow 會確認 tag commit 位於 `main`、只建置 `arm64-v8a`、執行測試與 lint、產生無 `DEBUG` 字樣的 release icon、驗證兩個 APK 的簽章，最後上傳主程式、Chewing plugin 與 `SHA256SUMS.txt`。版本含 `-preview`、`-alpha`、`-beta` 或其他 SemVer pre-release suffix 時會標記為 GitHub pre-release。

## English

Production APKs must use one stable, private Android signing key. If the key or password is lost, existing installations cannot upgrade to an APK signed with a different key. Keep separate offline backups of the keystore and credentials, and never commit either one to Git.

For first-time setup, generate the keystore in a trusted offline environment using the command above. Then create the same three repository secrets under **Settings → Secrets and variables → Actions**. Also create the `ANDROID_SIGN_CERT_SHA256` repository variable containing the lowercase signing-certificate SHA-256 fingerprint without colons. The workflow rejects an APK signed by any other certificate.

After merging and verifying the `main` CI run, create and push an annotated version tag as shown above. The `Android Release` workflow verifies that the tagged commit belongs to `main`, builds only `arm64-v8a`, runs tests and lint, uses the clean release launcher icon, verifies both APK signatures, and publishes the app, Chewing plugin, and `SHA256SUMS.txt`. Tags with a SemVer pre-release suffix are published as GitHub pre-releases.
