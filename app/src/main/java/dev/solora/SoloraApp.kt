package dev.solora

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dev.solora.data.AppDatabase
import dev.solora.i18n.I18n
import dev.solora.settings.LangStore
import dev.solora.sync.SyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SoloraApp : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.get(this)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
            
        // Apply persisted language at startup
        CoroutineScope(Dispatchers.Default).launch {
            val lang = LangStore.flow(this@SoloraApp).first()
            I18n.wrap(this@SoloraApp, lang)
        }
        
        // Schedule initial sync work
        SyncService.scheduleSyncWork(this)
    }
}