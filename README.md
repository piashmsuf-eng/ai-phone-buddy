<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=for-the-badge&logo=android" />
  <img src="https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?style=for-the-badge&logo=kotlin" />
  <img src="https://img.shields.io/badge/Gemini_Live-WebSocket-4285F4?style=for-the-badge&logo=google" />
  <img src="https://img.shields.io/badge/Voice-AI_Native_Audio-red?style=for-the-badge" />
</p>

<h1 align="center">🤖 MYRA — AI Voice Assistant</h1>
<h3 align="center">Control your Android phone with just your voice</h3>

<p align="center">
  <b>Hinglish + English • Hands-Free • AI-Powered</b><br>
  <i>Open Source • No Monthly Fees</i>
</p>

<p align="center">
  <a href="#-features"><img src="https://img.shields.io/badge/Features-View-brightgreen?style=for-the-badge" /></a>
  <a href="#%EF%B8%8F-how-it-works"><img src="https://img.shields.io/badge/How_It_Works-Learn-blue?style=for-the-badge" /></a>
  <a href="#-install"><img src="https://img.shields.io/badge/Download-APK-orange?style=for-the-badge&logo=android" /></a>
</p>

---

## 🎯 What is MYRA?

MYRA is an AI-powered voice assistant for Android that can **see your screen, understand your voice, and control your phone** — just like a human would. Speak in **Hinglish (Hindi+English)** or English, and MYRA does the rest.

> *"Hey MYRA, YouTube kholo" → YouTube opens.*
> *"WhatsApp karo Mom ko" → WhatsApp opens to Mom's chat.*
> *"Call my close friend" → Calls your prime contact.*

---

## ✨ Features

### 🎙️ Voice Control
| Feature | Description |
|---|---|
| 🎤 **Live Voice Chat** | Real-time voice conversation via Gemini Live WebSocket |
| 🗣️ **Native Human Voice** | MYRA speaks in natural human-like voice (8 voices!) |
| 🇮🇳 **Hinglish Support** | Mix Hindi + English naturally — "haan", "acha", "bilkul" |
| 💬 **Text Chat** | Text interface also available |
| 🔇 **Mute Toggle** | Tap mic to mute/unmute anytime |

### 📱 Phone Control
| Feature | Description |
|---|---|
| 📂 **Open/Close Apps** | YouTube, WhatsApp, Instagram, Chrome, Spotify, 30+ apps |
| 📞 **Make Calls** | Call contacts by name or prime contacts |
| 💬 **Send WhatsApp** | Open WhatsApp chat with message |
| 🔊 **Volume Control** | "volume badhao" / "volume down" |
| 🔦 **Flashlight** | "torch on karo" / "flashlight off" |
| 📶 **WiFi / Bluetooth** | Toggle on/off via voice |

### 👁️ Smart Detection
| Feature | Description |
|---|---|
| 📲 **Incoming Call Announce** | MYRA speaks who's calling, you say "uthao" or "reject" |
| ⭐ **Prime Contacts** | Save special contacts for quick access |

### 🎨 Beautiful UI
| Feature | Description |
|---|---|
| 🔴 **Animated Orb** | 4 states: idle pulse, listening red, speaking purple, thinking cyan |
| 🌊 **Live Waveform** | 20-bar waveform reacts to voice amplitude |
| 🌙 **Dark Theme** | Elegant black + red + purple design |
| 🫧 **Floating Overlay** | Double-press power button for quick orb access |

### 🧠 Personality Modes
| Mode | Language | Tone |
|---|---|---|
| 💖 **GF Mode** (Default) | Hinglish | Warm, caring, emotionally expressive |
| 💼 **Professional** | English | Formal, precise, efficient |
| 🤖 **Assistant** | Hinglish | Friendly, balanced, helpful |

---

## 🛠️ How It Works

```
Your Voice → Mic (16kHz PCM) → Gemini Live WebSocket → AI Response
                                                              ↓
Phone Actions ← Accessibility Service ← Parsed Command ← Voice Output (24kHz)
```

MYRA uses **Gemini Live WebSocket API** for real-time voice interaction and the **Android Accessibility Service** to see and control apps on your screen.

---

## 📱 Screenshots

<p align="center">
  <table>
    <tr>
      <td align="center"><b>🏠 Main Screen</b></td>
      <td align="center"><b>⚙️ Settings</b></td>
    </tr>
    <tr>
      <td align="center"><i>Orb • Waveform • Chat • Mic Control</i></td>
      <td align="center"><i>API Key • Voice • Personality • Prime Contacts</i></td>
    </tr>
  </table>
</p>

---

## 📥 Install

### Option 1: Build from Source
```bash
git clone https://github.com/piashmsuf-eng/ai-phone-buddy.git
cd ai-phone-buddy
./gradlew :app:assembleDebug
# APK → app/build/outputs/apk/debug/
```

### Option 2: Download APK
[⬇️ Download MYRA v1.0.5 APK](https://github.com/piashmsuf-eng/ai-phone-buddy/releases/download/v1.0.5/MYRA-v1.0.5.apk) (5 MB)

### Requirements
| Item | Detail |
|---|---|
| Android | 8.0+ (API 26) |
| Gemini API Key | Free from [Google AI Studio](https://aistudio.google.com/apikey) |
| Accessibility Service | Must be enabled in Settings |

---

## ⚙️ Setup

1. **Get API Key** from [Google AI Studio](https://aistudio.google.com/apikey)
2. Open MYRA → Settings → Paste API Key
3. Choose voice, model, and personality
4. Enable **Accessibility Service** in phone Settings
5. Say **"Hey MYRA"** and start controlling your phone!

---

## 📁 Tech Stack

```
Kotlin 1.9.24         — Language
MVVM Architecture     — Design Pattern
OkHttp 4.12          — WebSocket Client
Android Accessibility — Screen Control
Gemini Live API      — AI Voice Engine
Native PCM Audio     — 16kHz mic, 24kHz speaker
```

---

## 🗺️ Roadmap

| Priority | Feature |
|---|---|
| 🔴 P1 | Local LLM support (llama.cpp) — no internet needed |
| 🔴 P1 | Bangla voice support (TTS + STT) |
| 🟡 P2 | Multi-language UI (Bangla, Hindi) |
| 🟡 P2 | Task automation chains |
| 🟢 P3 | Health/fitness integration |
| 🟢 P3 | Home screen widget |

---

## 👨‍💻 Developer

<div align="center">

### Shorif Uddin Piash

**[fb.com/piashmsuf](https://fb.com/piashmsuf)**

*Made with ❤️ in Bangladesh*

</div>

---

## ⚖️ License

MIT License — free to use, modify, and distribute.
