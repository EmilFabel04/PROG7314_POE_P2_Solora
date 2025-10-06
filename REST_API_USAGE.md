# REST API Usage Documentation

## Overview
This document describes how the Solora app uses the REST API (Firebase Cloud Functions) with Firestore fallback.

## Current Implementation Status

### ✅ Fully Implemented with REST API

#### Quotes Operations
1. **GET Quotes** (`getQuotes`)
   - Primary: `FirebaseFunctionsApi.getQuotes()` 
   - Fallback: Direct Firestore `getQuotes()` flow
   - Used in: `QuotesViewModel`, `DashboardViewModel`, `HomeFragment`

2. **SAVE Quote** (`saveQuote`)
   - Primary: `FirebaseFunctionsApi.saveQuote()`
   - Fallback: Direct Firestore `firestore.collection("quotes").add()`
   - Used in: `QuotesViewModel.saveQuoteFromCalculation()`

3. **GET Quote by ID** (`getQuoteById`)
   - Primary: `FirebaseFunctionsApi.getQuoteById()`
   - Fallback: Direct Firestore `firestore.collection("quotes").document(id).get()`
   - Used in: `FirebaseRepository.getQuoteById()`

#### Leads Operations
1. **GET Leads** (`getLeads`)
   - Primary: `FirebaseFunctionsApi.getLeads()`
   - Fallback: Direct Firestore `getLeads()` flow
   - Used in: `LeadsViewModel`

2. **SAVE Lead** (`saveLead`)
   - Primary: `FirebaseFunctionsApi.saveLead()`
   - Fallback: Direct Firestore `firestore.collection("leads").add()`
   - Used in: `LeadsViewModel.createLeadFromQuote()`, `LeadsViewModel.createLeadFromQuoteSync()`

3. **GET Lead by ID** (`getLeadById`)
   - Primary: `FirebaseFunctionsApi.getLeadById()`
   - Fallback: Direct Firestore `firestore.collection("leads").document(id).get()`
   - Used in: `LeadsViewModel.linkQuoteToLeadSync()`

4. **UPDATE Lead** (`updateLead`)
   - Primary: `FirebaseFunctionsApi.updateLead()`
   - Fallback: Direct Firestore `firestore.collection("leads").document(id).update()`
   - Used in: `LeadsViewModel.linkQuoteToLeadSync()`, `FirebaseRepository.updateLead()`

#### Settings Operations
1. **GET Settings** (`getSettings`)
   - Primary: `FirebaseFunctionsApi.getSettings()`
   - Fallback: Direct Firestore via `SettingsRepository`
   - Used in: `SettingsViewModel`, `SettingsRepository`

2. **SYNC Data** (`syncData`)
   - Primary: `FirebaseFunctionsApi.syncData()`
   - Used in: Data synchronization operations

### ⚠️ Firestore Only (No REST API Endpoint)

These operations use direct Firestore because there are no corresponding REST API endpoints:

1. **DELETE Quote** (`deleteQuote`)
   - Direct Firestore only
   - Reason: No REST API endpoint for deletion

2. **DELETE Lead** (`deleteLead`)
   - Direct Firestore only
   - Reason: No REST API endpoint for deletion

## Implementation Pattern

All operations follow this pattern:

```kotlin
suspend fun operation(): Result<T> {
    return try {
        // Try REST API first
        val apiResult = apiService.operation()
        if (apiResult.isSuccess) {
            android.util.Log.d(TAG, "Using API for operation")
            apiResult
        } else {
            android.util.Log.w(TAG, "API failed, using direct Firestore")
            // Fallback to direct Firestore
            firestore.collection("...").operation()
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "API error, using direct Firestore")
        // Fallback to direct Firestore
        firestore.collection("...").operation()
    }
}
```

## Benefits of Current Approach

1. **Reliability**: Firestore fallback ensures operations complete even if API is down
2. **Performance**: REST API is tried first for better performance and centralized logic
3. **Consistency**: All data operations go through the same pattern
4. **Security**: REST API endpoints handle authentication and authorization
5. **Logging**: All operations log whether they use API or Firestore

## ViewModels Using REST API

1. **QuotesViewModel**
   - Uses `getQuotesViaApi()` for quotes flow
   - Uses `saveQuoteViaApi()` for saving quotes
   - Falls back to Firestore when API fails

2. **LeadsViewModel**
   - Uses `getLeadsViaApi()` for leads flow
   - Uses `saveLeadViaApi()` for creating leads
   - Uses `updateLead()` (which uses REST API) for linking quotes
   - Falls back to Firestore when API fails

3. **DashboardViewModel**
   - Uses `getQuotesViaApi()` for dashboard data
   - Falls back to Firestore when API fails

4. **SettingsViewModel**
   - Uses `updateSettingsViaApi()` for updating settings
   - Falls back to Firestore when API fails

5. **HomeFragment**
   - Uses `apiService.getQuotes()` directly for recent quotes
   - Falls back to ViewModel (which uses Firestore) when API fails

## API Service Classes

1. **FirebaseFunctionsApi** (`api/FirebaseFunctionsApi.kt`)
   - Primary REST API client for Firebase Cloud Functions
   - Handles all CRUD operations for quotes, leads, and settings
   - Uses Firebase authentication tokens

2. **FirebaseRepository** (`data/FirebaseRepository.kt`)
   - Orchestrates REST API calls with Firestore fallback
   - Handles data transformation and error handling
   - Manages user authentication

## Conclusion

✅ **The app fully uses REST API where available with proper Firestore fallback**

All major operations (GET, SAVE, UPDATE) use REST API as the primary method with automatic fallback to Firestore for reliability. Only DELETE operations use direct Firestore due to lack of REST API endpoints, which is an acceptable implementation choice for data integrity and security.

