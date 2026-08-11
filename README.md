# Native APK Builder

**Native APK Builder** is a production-grade native Android application that allows users to compile real Android APKs directly from their phone using GitHub Actions as a cloud build runner — without opening GitHub, GitHub Actions, Android Studio, Gradle, or a terminal.

---

## Table of Contents
1. [How to Build Native APK Builder](#1-how-to-build-native-apk-builder)
2. [How GitHub Authentication Works](#2-how-github-authentication-works)
3. [GitHub App vs GitHub OAuth App](#3-github-app-vs-github-oauth-app)
4. [How to Configure the GitHub Application](#4-how-to-configure-the-github-application)
5. [Required GitHub Permissions](#5-required-github-permissions)
6. [Deep Link & OAuth Redirect Mechanics](#6-deep-link--oauth-redirect-mechanics)
7. [Authentication Backend Configuration](#7-authentication-backend-configuration)
8. [Connecting GitHub from the Android App](#8-connecting-github-from-the-android-app)
9. [Build Repository Selection & Creation](#9-build-repository-selection--creation)
10. [How "Use This Folder" (SAF) Works](#10-how-use-this-folder-saf-works)
11. [Project File Upload Mechanism](#11-project-file-upload-mechanism)
12. [How GitHub Actions Executes the Build](#12-how-github-actions-executes-the-build)
13. [Debug APK Builds](#13-debug-apk-builds)
14. [Release APK Builds](#14-release-apk-builds)
15. [Downloading & Installing APK Artifacts](#15-downloading--installing-apk-artifacts)
16. [GitHub Actions Free-Tier Limitations](#16-github-actions-free-tier-limitations)
17. [Security Considerations](#17-security-considerations)

---

### 1. How to Build Native APK Builder
Open the project root in Android Studio or build using Gradle:
```bash
./gradlew assembleDebug
```
The resulting APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

### 2. How GitHub Authentication Works
Native APK Builder implements a real OAuth 2.0 flow:
1. User clicks **Connect GitHub**.
2. App opens GitHub's authorization page in a secure browser custom tab:
   `https://github.com/login/oauth/authorize?client_id=<CLIENT_ID>&scope=repo%20workflow&redirect_uri=nativeapkbuilder://oauth`
3. User authorizes the application.
4. GitHub redirects back to the Android app via deep-link scheme: `nativeapkbuilder://oauth?code=...`
5. The app receives the authorization code and exchanges it via OAuth API for an access token.
6. The token is verified against `https://api.github.com/user` to fetch account details (`@username`, avatar).
7. Token is stored securely in encrypted storage.

Alternatively, the app supports:
- **Device Authorization Flow**: User receives a short code (e.g. `A1B2-C3D4`) and completes auth on `github.com/login/device`.
- **Personal Access Token (PAT)**: Power users can paste a PAT with `repo` and `workflow` scopes directly.

---

### 3. GitHub App vs GitHub OAuth App
This application uses a **GitHub OAuth App** (with Public Client OAuth PKCE / Device Flow support):
- **Why OAuth App?**: Allows per-user OAuth authentication where each user connects their own personal GitHub account and triggers builds in their own private repositories.
- **Why Public Client Flow?**: Keeps client secrets server-side or off-device to ensure secrets are never embedded inside the APK.

---

### 4. How to Configure the GitHub Application
To register your own GitHub OAuth App:
1. Go to **GitHub Settings → Developer Settings → OAuth Apps → New OAuth App**.
2. Application Name: `Native APK Builder`
3. Homepage URL: `https://nativeapkbuilder.app`
4. Authorization Callback URL: `nativeapkbuilder://oauth`
5. Copy your **Client ID** into `GitHubAuthManager.DEFAULT_CLIENT_ID`.

---

### 5. Required GitHub Permissions
The app requests only minimal necessary scopes:
- `repo`: Required to stage project files and create/update workflow files in your designated build repository.
- `workflow`: Required to trigger `workflow_dispatch` events and monitor GitHub Actions workflow runs.

---

### 6. Deep Link & OAuth Redirect Mechanics
Configured in `app/src/main/AndroidManifest.xml`:
```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:host="oauth" android:scheme="nativeapkbuilder" />
</intent-filter>
```
When GitHub redirects to `nativeapkbuilder://oauth?code=XYZ`, Android routes the intent back to `MainActivity.onNewIntent()`.

---

### 7. Authentication Backend Configuration
For production deployments where an OAuth Client Secret is used:
- The Android app sends the `code` to a lightweight proxy server (e.g., Cloudflare Worker or Vercel Function).
- The proxy exchanges `code` + `CLIENT_SECRET` with `https://github.com/login/oauth/access_token` and returns the token securely to the app.

---

### 8. Connecting GitHub from the Android App
In the **GitHub** tab:
1. Press **Connect GitHub Account**.
2. Authorize via browser, Device Code, or PAT.
3. Once authenticated, the app displays `✓ Connected @username`.

---

### 9. Build Repository Selection & Creation
- The app lets users choose an existing repository or tap **Create Private Build Repository**.
- Default repository name: `native-apk-builder` (created as **PRIVATE** by default).

---

### 10. How "Use This Folder" (SAF) Works
- Tapping `📁 Use This Folder` triggers Android's `ACTION_OPEN_DOCUMENT_TREE`.
- The user selects an Android project directory (e.g., exported from Google AI Studio).
- `ProjectScanner` recursively traverses the directory, detects `settings.gradle(.kts)`, `build.gradle(.kts)`, `gradlew`, SDK versions (`compileSdk`, `minSdk`, `targetSdk`), and application modules (`app`, `mobile`, `androidApp`, etc.).

---

### 11. Project File Upload Mechanism
`ProjectUploader` uses GitHub's Git Database API for fast batch uploads:
1. Filters out unnecessary build clutter (`.gradle/`, `build/`, `.idea/`, `local.properties`, `*.jks`, `*.keystore`, `.env`).
2. Creates blob items for project source files.
3. Creates a new Git Tree (`POST /repos/{owner}/{repo}/git/trees`).
4. Creates a commit (`POST /repos/{owner}/{repo}/git/commits`).
5. Updates the repository branch reference (`PATCH /repos/{owner}/{repo}/git/refs/heads/main`).

---

### 12. How GitHub Actions Executes the Build
The app automatically injects `.github/workflows/android-build.yml` into the repository:
```yaml
name: Android APK Build
on:
  workflow_dispatch:
    inputs:
      build_type:
        default: 'debug'
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '17' }
      - uses: android-actions/setup-android@v3
      - run: chmod +x gradlew || true
      - run: ./gradlew assembleDebug --no-daemon
      - uses: actions/upload-artifact@v4
        with: { name: debug-apk, path: '**/build/outputs/apk/debug/*.apk' }
```

---

### 13. Debug APK Builds
Selecting **Debug APK** triggers `assembleDebug`. This builds a standard debug APK signed with Android's default debug key.

---

### 14. Release APK Builds
Selecting **Release APK** triggers `assembleRelease`. If no release keystore is configured in the project's `build.gradle`, it falls back gracefully to `assembleUnsignedRelease`.

---

### 15. Downloading & Installing APK Artifacts
1. The app polls workflow status (`QUEUED` -> `IN_PROGRESS` -> `COMPLETED`).
2. Upon completion, `GitHubActionsBuildEngine` lists artifacts and downloads `debug-apk.zip`.
3. Extracts `app-debug.apk` to app storage.
4. Uses `ApkInstaller` and Android `FileProvider` to prompt system package installation:
   ```kotlin
   val installIntent = Intent(Intent.ACTION_VIEW).apply {
       setDataAndType(apkUri, "application/vnd.android.package-archive")
       addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
   }
   ```

---

### 16. GitHub Actions Free-Tier Limitations
- GitHub provides 2,000 free Actions build minutes per month for free personal accounts on private repositories.
- Standard Android Gradle build duration: ~1.5 to 3 minutes per build.

---

### 17. Security Considerations
- **No Hardcoded Tokens**: GitHub tokens are never stored in source code or logged.
- **Encrypted Local Storage**: Tokens are saved using Android Keystore / encrypted preferences.
- **Private Repositories**: Uploaded code is pushed to user's private repository.
- **Permissions**: Minimum required scopes (`repo`, `workflow`).
