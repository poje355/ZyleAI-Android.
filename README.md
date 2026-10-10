# Zyle AI — Android starter project

A simple native Android chat UI with neon terminal styling, local chat history, a Google Search shortcut, and an optional Gemini-powered backend.

## Requirements
- Android Studio (recent stable release)
- Android SDK 35
- Node.js 20+ for the backend
- A Gemini API key from Google AI Studio

## 1. Open the Android project
1. Unzip the project.
2. Open the `ZyleAI_Android` folder in Android Studio.
3. Let Gradle sync finish.
4. In `app/src/main/java/com/zyleai/app/MainActivity.kt`, change:
   `https://YOUR-BACKEND-DOMAIN.example/chat`
   to your deployed backend URL ending in `/chat`.
5. Run on an Android phone with USB debugging enabled, or use an emulator.

## 2. Run the backend locally
1. Install Node.js.
2. Open a terminal in the `backend` folder.
3. Run `npm install`.
4. Copy `.env.example` to `.env`.
5. Put your key in `.env` as `GEMINI_API_KEY=...`.
6. Run `npm start`.

The Android app cannot reach `localhost` on your computer when installed on a phone. For local testing, deploy the backend to a trusted HTTPS host, or configure a development network URL carefully. The app intentionally uses HTTPS and does not embed your API key.

## 3. Create the API key
Use https://aistudio.google.com/apikey . If Google AI Studio says permission denied, resolve account/project access first. Never paste the API key into the Android Kotlin source, screenshots, or public repositories.

## Included features
- Neon terminal-style chat interface
- Coding questions through Gemini backend
- Google Search shortcut: type a query in the input, then tap CARI WEB
- Last 200 chat messages saved locally on the device
- New Chat button (does not erase saved history)

## Notes / limitations
- This is a starter source project, not a prebuilt APK.
- The Search button opens Google Search in the browser; it does not automatically summarize web pages.
- The backend currently sends each message independently (no multi-turn AI context yet).
- For production use, restrict CORS, add authentication, configure quotas, and deploy over HTTPS.
