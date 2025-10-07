package dev.solora

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class SoloraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
    }
}