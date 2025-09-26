package dev.solora.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Lead::class, Quote::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun leadDao(): LeadDao
    abstract fun quoteDao(): QuoteDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        // Migration from version 4 to 5 - adding consultantId and quoteId fields
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add consultantId to quotes table
                database.execSQL("ALTER TABLE quotes ADD COLUMN consultantId TEXT")
                
                // Add consultantId and quoteId to leads table
                database.execSQL("ALTER TABLE leads ADD COLUMN consultantId TEXT")
                database.execSQL("ALTER TABLE leads ADD COLUMN quoteId INTEGER")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "solora.db"
                )
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
    }
}


