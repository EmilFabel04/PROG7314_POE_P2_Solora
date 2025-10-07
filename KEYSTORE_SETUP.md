# Solora Build and Testing Guide

## For Lecturers and Evaluators

This document provides instructions to build and test the Solora Android application.

### 1. Building the Application

**Debug Build (Recommended for Testing):**
```bash
./gradlew assembleDebug
```
The debug APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

**Note**: Debug builds don't require a keystore and are perfect for testing and evaluation.

### 2. Pre-built APK
A pre-built debug APK is available at: `app/debug/app-debug.apk`

### 3. Installation
Install the APK on an Android device or emulator:
```bash
adb install app/debug/app-debug.apk
```

### 4. Authentication Setup
- **Email/Password**: Can be used immediately for testing
- **Google Sign-In**: Works with debug keystore (configured in Firebase)
- **Firebase Project**: Uses the included `google-services.json` configuration

### 5. Testing the Application
1. Install the APK on an Android device (API level 24+)
2. Create a test account using Email/Password authentication
3. Test core features:
   - Quote calculation with NASA solar data integration
   - Lead management and search functionality
   - PDF generation and sharing
   - Dashboard analytics with date filtering
   - User profile and settings management

### 6. Firebase Configuration
- The project uses Firebase for authentication, database, and cloud functions
- All necessary configurations are included in the repository
- No additional setup required for basic functionality testing
- Cloud Functions provide server-side logic for calculations and data processing

### 7. Key Features to Test
- **Onboarding Flow**: First-time user experience
- **Authentication**: Email/password and Google Sign-In
- **Quote Calculator**: Solar system sizing with real-time calculations
- **Lead Management**: CRUD operations with search and filtering
- **PDF Generation**: Professional quote documents
- **Dashboard**: Analytics and performance metrics
- **Offline Support**: App functionality without internet connection

## Notes for Academic Evaluation
- All source code is available for review
- The application demonstrates modern Android development practices
- Firebase integration showcases cloud-based architecture
- REST API integration with fallback mechanisms
- Comprehensive error handling and user feedback
- Material Design implementation
- MVVM architecture pattern
