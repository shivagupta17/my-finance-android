package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Bill
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionPayment
import com.example.data.model.BillPayment

@Database(entities = [Bill::class, Subscription::class, SubscriptionPayment::class, BillPayment::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun subscriptionPaymentDao(): SubscriptionPaymentDao
    abstract fun billPaymentDao(): BillPaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bills ADD COLUMN status TEXT NOT NULL DEFAULT 'Active'")
            }
        }

        fun setTestDatabase(testDb: AppDatabase?) {
            INSTANCE = testDb
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bill_sub_tracker_db"
                )
                .addMigrations(MIGRATION_7_8)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
