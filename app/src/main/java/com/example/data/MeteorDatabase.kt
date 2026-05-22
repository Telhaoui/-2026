package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "historical_falls")
data class HistoricalFall(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nameAr: String,
    val nameEn: String,
    val locationAr: String,
    val year: String,
    val mass: String,
    val typeAr: String,
    val descriptionAr: String,
    val latitude: Double,
    val longitude: Double,
    val impactEnergy: String = "غير معروفة"
)

@Entity(tableName = "community_discoveries")
data class DiscoveryPost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val observerName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val locationName: String,
    val notesAr: String,
    val classificationAr: String,
    val estimatedTrajectoryAr: String = "غير محدد",
    val microAnalysisAr: String? = null,
    val sampleImageCode: String = "default" // "iron", "stony", "pallasite", "glassy"
)

// --- DAOs ---

@Dao
interface MeteorDao {
    // Historical Falls
    @Query("SELECT * FROM historical_falls ORDER BY year DESC")
    fun getAllHistoricalFalls(): Flow<List<HistoricalFall>>

    @Query("SELECT * FROM historical_falls WHERE nameAr LIKE :searchQuery OR nameEn LIKE :searchQuery OR locationAr LIKE :searchQuery")
    fun searchHistoricalFalls(searchQuery: String): Flow<List<HistoricalFall>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoricalFall(fall: HistoricalFall)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoricalFalls(falls: List<HistoricalFall>)

    // Community Discoveries
    @Query("SELECT * FROM community_discoveries ORDER BY timestamp DESC")
    fun getAllDiscoveries(): Flow<List<DiscoveryPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscovery(discovery: DiscoveryPost)

    @Query("DELETE FROM community_discoveries WHERE id = :id")
    suspend fun deleteDiscoveryById(id: Int)
}

// --- Database ---

@Database(entities = [HistoricalFall::class, DiscoveryPost::class], version = 1, exportSchema = false)
abstract class MeteorDatabase : RoomDatabase() {
    abstract fun meteorDao(): MeteorDao
}

// --- Repository ---

class MeteorRepository(private val meteorDao: MeteorDao) {
    val allHistoricalFalls: Flow<List<HistoricalFall>> = meteorDao.getAllHistoricalFalls()
    val allDiscoveries: Flow<List<DiscoveryPost>> = meteorDao.getAllDiscoveries()

    fun searchFalls(query: String): Flow<List<HistoricalFall>> {
        return meteorDao.searchHistoricalFalls("%$query%")
    }

    suspend fun insertFall(fall: HistoricalFall) = meteorDao.insertHistoricalFall(fall)
    
    suspend fun insertFalls(falls: List<HistoricalFall>) = meteorDao.insertHistoricalFalls(falls)

    suspend fun insertDiscovery(post: DiscoveryPost) = meteorDao.insertDiscovery(post)

    suspend fun deleteDiscovery(id: Int) = meteorDao.deleteDiscoveryById(id)

    suspend fun prepopulateDatabaseIfEmpty() {
        // Can be called to populate initial historic records
    }
}
