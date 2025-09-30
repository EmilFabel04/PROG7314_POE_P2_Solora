# Solora Release Keystore Setup

## For Team Members

### 1. Get the Release Keystore
- Contact Emil or Zimkhitha to get the `solora-release-key.keystore` file
- Place it in `app/keystore/solora-release-key.keystore`

### 2. Keystore Details
- **File**: `app/keystore/solora-release-key.keystore`
- **Alias**: `solora`
- **Password**: `SoloraPOE2025`
- **Key Password**: `SoloraPOE2025`

### 3. Build Release APK
```bash
./gradlew assembleRelease
```

### 4. Google SSO
- The release keystore SHA-1 is already added to Firebase
- Google Sign-In will work for all team members using this keystore

## Security Notes
- Never commit the keystore file to Git
- Share the keystore file securely (encrypted file sharing)
- Keep passwords secure and share separately from the keystore file

## Firebase Configuration
- SHA-1 fingerprint from release keystore is configured in Firebase Console
- No need to add individual developer SHA-1 fingerprints
