package dev.solora.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.solora.SoloraApp
import dev.solora.api.ApiRepository
import dev.solora.api.LeadData
import dev.solora.api.QuoteData
import dev.solora.auth.AuthRepository
import dev.solora.data.Lead
import dev.solora.data.Quote
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SyncService(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val apiRepository: ApiRepository
) {
    companion object {
        const val SYNC_WORK_NAME = "solora_sync_work"
        
        fun scheduleSyncWork(context: Context) {
            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInitialDelay(5, TimeUnit.SECONDS)
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )
        }
    }

    suspend fun syncAllData(): Result<String> {
        try {
            // Check if user is logged in
            if (!authRepository.isLoggedIn.first()) {
                return Result.failure(Exception("User not logged in"))
            }

            val user = authRepository.currentUser
                ?: return Result.failure(Exception("No current user"))

            // Get Firebase ID token for API authentication
            val token = user.getIdToken(false).result?.token
                ?: return Result.failure(Exception("Failed to get auth token"))

            val database = (context.applicationContext as SoloraApp).database

            // Sync quotes
            val localQuotes = database.quoteDao().observeQuotes().first()
            val quoteSyncResult = syncQuotes(token, localQuotes)
            
            // Sync leads
            val localLeads = database.leadDao().observeLeads().first()
            val leadSyncResult = syncLeads(token, localLeads)

            // Load configuration
            val configResult = apiRepository.syncConfiguration(token)

            val results = mutableListOf<String>()
            quoteSyncResult.fold(
                onSuccess = { results.add("Quotes: ${it.syncedCount} synced") },
                onFailure = { results.add("Quotes: Failed - ${it.message}") }
            )
            
            leadSyncResult.fold(
                onSuccess = { results.add("Leads: ${it.syncedCount} synced") },
                onFailure = { results.add("Leads: Failed - ${it.message}") }
            )

            configResult.fold(
                onSuccess = { results.add("Configuration updated") },
                onFailure = { results.add("Config: Failed - ${it.message}") }
            )

            apiRepository.markLastSync()
            
            return Result.success("Sync completed: ${results.joinToString(", ")}")
            
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private suspend fun syncQuotes(token: String, localQuotes: List<Quote>): Result<dev.solora.api.SyncResponse> {
        val quoteData = localQuotes.map { quote ->
            QuoteData(
                id = quote.id.toString(),
                reference = quote.reference,
                clientName = quote.clientName,
                address = quote.address,
                usageKwh = quote.monthlyUsageKwh,
                billRands = quote.monthlyBillRands,
                tariff = quote.tariff,
                panelWatt = quote.panelWatt,
                sunHours = quote.sunHours,
                systemKwp = quote.systemKw,
                estimatedGeneration = quote.systemKw * quote.sunHours * 30, // Estimate
                paybackMonths = if (quote.savingsRands > 0) (quote.systemKw * 1000 / quote.savingsRands * 12).toInt() else 0, // Estimate
                savingsFirstYear = quote.savingsRands * 12,
                dateEpoch = quote.dateEpoch
            )
        }
        
        return apiRepository.syncQuotes(token, quoteData)
    }

    private suspend fun syncLeads(token: String, localLeads: List<Lead>): Result<dev.solora.api.SyncResponse> {
        val leadData = localLeads.map { lead ->
            LeadData(
                id = lead.id.toString(),
                name = lead.name,
                email = lead.contact, // Using contact as email fallback
                phone = lead.contact, // Using contact as phone fallback
                status = "new", // Default status
                notes = "Reference: ${lead.reference}, Address: ${lead.address}"
            )
        }
        
        return apiRepository.syncLeads(token, leadData)
    }
}

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val authRepository = AuthRepository(applicationContext)
            val apiRepository = ApiRepository(applicationContext)
            val syncService = SyncService(applicationContext, authRepository, apiRepository)
            
            val result = syncService.syncAllData()
            
            if (result.isSuccess) {
                Result.success(Data.Builder()
                    .putString("message", result.getOrNull())
                    .build())
            } else {
                Result.failure(Data.Builder()
                    .putString("error", result.exceptionOrNull()?.message ?: "Sync failed")
                    .build())
            }
        } catch (e: Exception) {
            Result.failure(Data.Builder()
                .putString("error", e.message ?: "Unknown error")
                .build())
        }
    }
}
