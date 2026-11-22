# JARVIS Android

A fresh native Android starter for the JARVIS personal assistant.

## Build
Push to `main` or run **Actions → Build JARVIS APK → Run workflow**. When the workflow succeeds, download the **JARVIS-APK** artifact from the run.

## Current starter features
- Native Kotlin + Jetpack Compose Android app
- Groq chat completion using `llama-3.3-70b-versatile`
- Speech-to-text through Android's installed recognition service
- UK English Android text-to-speech preference

## Important limitations
This is a starter, not the finished JARVIS. Groq requires an internet connection, so this AI brain is not offline. UK English TTS is requested, but the phone's installed voice determines the actual voice and gender. Always-on wake-word listening and phone-control actions are not implemented yet.

Never put a Groq API key in source code or GitHub. The app currently keeps the key only in screen memory while it is open. Future phone actions must show a confirmation before execution.
