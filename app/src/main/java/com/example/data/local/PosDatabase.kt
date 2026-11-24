package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MenuItemEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        PaymentTransactionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun posDao(): PosDao

    companion object {
        @Volatile
        private var INSTANCE: PosDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE orders ADD COLUMN paymentMode TEXT NOT NULL DEFAULT 'Paid in Full'")
                db.execSQL("ALTER TABLE orders ADD COLUMN paidPkr INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE orders SET paidPkr = totalPkr")
                db.execSQL("ALTER TABLE orders ADD COLUMN udharPkr INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE orders ADD COLUMN customerName TEXT")
                db.execSQL("ALTER TABLE orders ADD COLUMN customerPhone TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE orders ADD COLUMN transactionId TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `payment_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `orderId` INTEGER NOT NULL,
                        `amountPkr` INTEGER NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `transactionId` TEXT,
                        `note` TEXT NOT NULL,
                        `paidAt` INTEGER NOT NULL,
                        FOREIGN KEY(`orderId`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_payment_transactions_orderId` ON `payment_transactions` (`orderId`)"
                )
                db.execSQL(
                    """
                    INSERT INTO `payment_transactions` (`orderId`, `amountPkr`, `paymentMethod`, `transactionId`, `note`, `paidAt`)
                    SELECT `id`, `paidPkr`, `paymentMethod`, `transactionId`,
                        CASE WHEN `udharPkr` > 0 THEN 'Initial Payment' ELSE 'Full Payment' END,
                        `createdAt`
                    FROM `orders`
                    WHERE `paidPkr` > 0
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): PosDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PosDatabase::class.java,
                    "flame_pos.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
