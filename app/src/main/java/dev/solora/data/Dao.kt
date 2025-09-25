package dev.solora.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LeadDao {
    @Query("SELECT * FROM leads ORDER BY createdAt DESC")
    fun observeLeads(): Flow<List<Lead>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lead: Lead): Long // Returns the generated ID
    
    @Query("UPDATE leads SET status = :status, notes = :notes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateLeadStatus(id: Long, status: String, notes: String, updatedAt: Long)
}

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes ORDER BY dateEpoch DESC")
    fun observeQuotes(): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE id = :id")
    fun observeQuote(id: Long): Flow<Quote?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quote: Quote): Long
}


