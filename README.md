# TranslateGemma — Context-Aware AI Voice Translator 🌍⚡

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![Gemma AI](https://img.shields.io/badge/AI%20Model-Google%20Gemma-orange.svg)](https://ai.google.dev/gemma)

**TranslateGemma** is a real-time, context-aware dual-speaker voice and text translation Android application powered by Google Gemma AI models. Built with **Jetpack Compose** and **Material 3**, it is engineered specifically for outdoor face-to-face communication with high-contrast UI elements, tactile giant controls, and ephemeral conversation memory.

---

## ✨ Key Features

- 💬 **Context-Aware Dialogue Intelligence**:
  - Maintains temporary, in-memory conversation context across turns (moderate context intensity).
  - Automatically resolves ambiguous pronouns (*it, that, they*), adapts conversational register, and preserves context continuity.
  - Ephemeral session memory: All context logs are cleared automatically when the app is closed.

- ☀️ **High-Contrast Outdoor Dual-Speaker UI**:
  - **Tourist Speaker Box**: Styled in vivid Pure Red (`#D32F2F`) with high-contrast white text.
  - **Local Speaker Box**: Styled in vivid Pure Green (`#2E7D32`) with high-contrast white text.
  - Formatted for instant visual differentiation even under direct sunlight.

- 🔄 **Animated Color & Language Swap**:
  - Large center switch button with a high-visibility black filled background and crisp 3dp white border.
  - Smooth 350ms color transition animating the top and bottom boxes between Red and Green as languages are swapped.

- 🎙️ **Giant Outdoor Tactile Microphones**:
  - Extra-large **150dp** solid black microphone buttons for effortless tap/hold gestures on the move.
  - Visual audio pulse indicator providing immediate feedback during voice capture.

- ⚡ **Google Gemma & Gemini AI Engine**:
  - Powered by Google Gemma 3 and 12B model architectures via cloud API.
  - On-Device Gemma 2B model manager for offline translation capability.

---

## 🛠️ Tech Stack & Architecture

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose with Material 3 Design
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Repository pattern
- **State Management**: Kotlin Coroutines & `StateFlow`
- **Networking**: Ktor / Retrofit HTTP Client
- **AI Integration**: Google Gemma AI REST API & On-Device Gemma Manager

---

## 🚀 Getting Started

### 📦 Quick Install (Pre-built APK)

You can download the pre-compiled Android APK directly from the **[GitHub Releases](https://github.com/YOUR_USERNAME/TranslateGemma/releases)** section:

1. Download the latest `app-release.apk` (or `app-debug.apk`) from the **Releases** tab.
2. Open the file on your Android device (ensure "Install from Unknown Sources" is enabled in settings if prompted).
3. Open **TranslateGemma** and start translating!

---

### 💻 Build from Source (For Developers)

#### Prerequisites

- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android SDK 24+ (Android 7.0+)

#### Steps

1. **Clone the repository**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/TranslateGemma.git
   cd TranslateGemma
   ```

2. **Configure API Key** *(Optional for Cloud AI features)*:
   - Create a `.env` file or set your Gemini/Gemma API key in `BuildConfig`.

3. **Build & Run**:
   - Open the project in Android Studio and press **Run** (`Shift + F10`).

---

## 🏷️ Recommended GitHub Repository Settings

- **Repository Title**: `TranslateGemma`
- **Short Description**: `Context-aware dual-speaker voice translator powered by Google Gemma AI with high-contrast outdoor UI & on-device support.`
- **Topics**: `android`, `jetpack-compose`, `gemma`, `kotlin`, `gemini-api`, `voice-translator`, `on-device-ai`, `material-design-3`

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
