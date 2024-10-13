package com.justself.klique
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "gistState")
data class GistStateEntity(
    @PrimaryKey val id: Int = 1, // Fixed primary key
    val gistId: String,
    val topic: String,
    val description: String,
    val startedBy: String,
    val startedById: Int,
    val isSpeaker: Boolean,
    val isOwner: Boolean,
    val gistImage: String,
    val activeSpectators: Int,
    val timestamp: Long = System.currentTimeMillis()
)
@Dao
interface GistStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGistState(gistState: GistStateEntity)

    @Query("SELECT * FROM gistState LIMIT 1")
    suspend fun getGistState(): GistStateEntity?

    @Query("DELETE FROM gistState WHERE id = :id")
    suspend fun deleteGistState(id: Int = 1)

    @Query("DELETE FROM gistState")
    suspend fun clearAllGistStates()
    @Query("UPDATE gistState SET timestamp = :newTimestamp WHERE id = 0")
    suspend fun updateTimestamp(newTimestamp: Long)
    @Query("UPDATE gistState SET activeSpectators = :newActiveSpectators WHERE id = 1")
    suspend fun updateActiveSpectators(newActiveSpectators: Int)
}
@Database(entities = [GistStateEntity::class], version = 1)
abstract class GistRoomCreatedBase : RoomDatabase() {
    abstract fun gistStateDao(): GistStateDao
}