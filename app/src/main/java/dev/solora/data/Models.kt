package dev.solora.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leads")
data class Lead(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val name: String,
    val address: String,
    val contact: String
)

@Entity(tableName = "quotes")
data class Quote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val leadId: Long?,
    val panels: Int,
    val systemKw: Double,
    val inverterKw: Double,
    val savingsRands: Double,
    val dateEpoch: Long = System.currentTimeMillis()
)


