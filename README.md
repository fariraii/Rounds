# 🏥 Rounds — Final Year Project Android App

**Rounds** is an Android mobile application built with **Kotlin** and **Firebase**, developed as part of my Final Year Project.  
The app demonstrates modern Android development practices, Firebase integration, and Material Design principles.

---

## ✨ Features
- 📱 Modern Android UI with Material Components  
- 🔐 Firebase Authentication (secure user login & signup)  
- ☁️ Firebase Realtime Database / Firestore integration  
- 📊 Data persistence and cloud syncing  
- 🛠️ Gradle Kotlin DSL build system  
- 🎯 Scalable project architecture for maintainability  

---

## 🛠️ Tech Stack
- **Language:** Kotlin  
- **Frameworks/Libraries:** AndroidX, Material Design  
- **Backend/Cloud:** Firebase (Auth, Firestore, Realtime Database, Cloud Messaging)  
- **Build System:** Gradle (KTS)  
- **IDE:** Android Studio  

---

## 📂 Project Structure
```
Rounds/
 ├── app/                  # Main Android application module
 ├── build.gradle.kts      # Gradle build script (project level)
 ├── settings.gradle.kts   # Gradle settings
 ├── .gitignore            # Ignored files (local configs, builds, secrets)
 ├── firebase.json         # Firebase config (not committed if sensitive)
 └── local.properties      # Local environment variables (untracked)
```

---

## 🚀 Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/fariraii/Rounds.git
cd Rounds
```

### 2. Open in Android Studio
- Use **Android Studio Iguana (or later)**.  
- Let Gradle sync automatically.  

### 3. Configure Firebase
- Place your `google-services.json` file inside the `app/` directory.  
- Ensure your Firebase project matches the app’s **package name**.  

### 4. Run the app
- Connect an Android device or start an emulator.  
- Click ▶️ **Run** in Android Studio.  

---

## 🔒 Security Notes
- API keys (e.g., Maps, Firebase) must **not** be hardcoded.  
- Add secrets to `local.properties` (ignored by Git) and reference them via `manifestPlaceholders`.  
- See `.gitignore` to confirm sensitive files (like `google-services.json`, `local.properties`, `*.keystore`) are excluded from version control.  

---

## 📜 License
This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.  

---

👤 **Author:** Farirai Charumbira  
📧 **Contact:** charumbirafarirai@gmail.com
