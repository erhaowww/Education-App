package com.example.kleine.model

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import android.util.Log
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.parcelize.Parcelize

@Database(entities = [PartnershipEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun partnershipDao(): PartnershipDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "partnership-database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


enum class PartnershipStatus {
    pending, approved, rejected, quit
}
@Parcelize
data class Partnership(
    var id: String = "",
    var userId: String,
    var instiName: String,
    var instiType: String,
    var location: String,
    var contactNum: String,
    var reason: String,
    var status: PartnershipStatus = PartnershipStatus.pending,
    var rejectReason: String = "",
    var documentation: String = "",
    var documentationName: String = ""
): Parcelable {
    constructor() : this("","", "", "", "", "", "", PartnershipStatus.pending, "", "", "")
    fun toEntity(): PartnershipEntity {
        return PartnershipEntity(
            id = this.id,
            instiName = this.instiName,
            instiType = this.instiType,
            location = this.location,
            contactNum = this.contactNum,
            reason = this.reason,
            documentation = this.documentation,
            documentationName = this.documentationName,
            userId = this.userId,
            status = this.status.toString(),
            documentationLocalPath = "|"
        )
    }
}

@Entity(tableName = "partnership")
data class PartnershipEntity(
    @PrimaryKey
    val id: String,
    val instiName: String,
    val instiType: String,
    val location: String,
    val contactNum: String,
    val reason: String,
    val documentation: String,
    val documentationName: String,
    var documentationLocalPath: String? = "|", // Added this field to store local file path
    val userId: String,
    val status: String
)



@Dao
interface PartnershipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(partnership: PartnershipEntity)

    @Query("SELECT * FROM partnership WHERE userId = :userId")
    suspend fun getPartnershipByUserId(userId: String): List<PartnershipEntity>

    @Query("SELECT documentationLocalPath FROM partnership WHERE id = :partnershipId")
    suspend fun getDocumentationLocalPath(partnershipId: String): String

    @Query("UPDATE partnership SET documentationLocalPath = :filePath WHERE id = :id")
    suspend fun updateDocumentationLocalPath(id: String, filePath: String)

    @Query("SELECT * FROM partnership WHERE id = :id LIMIT 1")
    suspend fun getPartnershipById(id: String): PartnershipEntity?

    @Query("""
    UPDATE partnership 
    SET instiName = :instiName, instiType = :instiType, location = :location, 
        contactNum = :contactNum, reason = :reason, documentation = :documentation,
        documentationName = :documentationName, userId = :userId, status = :status
    WHERE id = :id
""")
    suspend fun update(
        id: String,
        instiName: String,
        instiType: String,
        location: String,
        contactNum: String,
        reason: String,
        documentation: String,
        documentationName: String,
        userId: String,
        status: String
    )


}


val MIGRATION_1_2 = object : Migration(1, 2) {
    @SuppressLint("Range")
    override fun migrate(database: SupportSQLiteDatabase) {
        val cursor = database.query("PRAGMA table_info(partnership)")
        var columnExists = false
        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndex("name"))
            if ("documentationLocalPath" == name) {
                columnExists = true
                break
            }
        }
        cursor.close()

        if (!columnExists) {
            Log.d("Migration", "Adding column documentationLocalPath")
            database.execSQL("ALTER TABLE partnership ADD COLUMN documentationLocalPath TEXT DEFAULT NULL")
        } else {
            Log.d("Migration", "Column documentationLocalPath already exists")
        }

    }
}





