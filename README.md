# 🛠️ MahalleUstasi (Neighborhood Pro)

MahalleUstasi is a modern Android application connecting local service professionals (Ustalar) with neighbors who need help with home repairs, renovations, and other services. Built with **Jetpack Compose** and **Firebase**, it offers a seamless experience for posting jobs, receiving offers, and managing service requests.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-039BE5?style=for-the-badge&logo=Firebase&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)

## ✨ Features

- **🔐 Secure Authentication**: Email/Password login and registration via Firebase Auth.
- **📋 Job Management**:
    - Post detailed job requests with photos and location.
    - Browse available jobs in a list or on an interactive **Google Map**.
    - Filter jobs by category and status.
- **💬 Real-time Bidding & Chat**:
    - Professionals can submit offers for jobs.
    - Job owners can review, accept, or reject offers.
    - **Real-time chat** between job owners and professionals once an offer is discussed.
- **👤 User Profiles**:
    - Professional profiles with ratings and review statistics.
    - Edit profile details and upload avatars.
- **⭐ Reviews & Ratings**: Rate and review professionals after job completion.
- **🔔 Notifications**: Push notifications for new offers, messages, and job updates (FCM).
- **📍 Location Services**: Integrated location picker for accurate job positioning.

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
- **Asynchronous Programming**: [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **Backend (BaaS)**: [Firebase](https://firebase.google.com/)
    - Authentication
    - Firestore (NoSQL Database)
    - Storage (Image hosting)
    - Cloud Messaging (Notifications)
- **Navigation**: [Compose Navigation](https://developer.android.com/guide/navigation/navigation-compose)
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
- **Maps**: Google Maps SDK for Android (Maps Compose)

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog | 2023.1.1 or newer.
- JDK 17.
- A Firebase project.
- A Google Maps API Key.

### Setup

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/YOUR_USERNAME/MahalleUstasi.git
    ```
2.  **Firebase Setup:**
    - Create a project in the [Firebase Console](https://console.firebase.google.com/).
    - Add an Android app with package name `com.example.mahalleustasi`.
    - Download `google-services.json` and place it in the `app/` directory.
    - Enable **Authentication** (Email/Password).
    - Enable **Firestore** and **Storage**.
3.  **Maps API Key:**
    - Get an API Key from [Google Cloud Console](https://console.cloud.google.com/).
    - Enable "Maps SDK for Android".
    - Add your key to `local.properties` (recommended) or directly in `AndroidManifest.xml` (for testing).
    ```properties
    MAPS_API_KEY=your_api_key_here
    ```
4.  **Build & Run:**
    - Open the project in Android Studio.
    - Sync Gradle files.
    - Run on an emulator or physical device.

## 📸 Screenshots

*(Add screenshots of your app here)*

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
