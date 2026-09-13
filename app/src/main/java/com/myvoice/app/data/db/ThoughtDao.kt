package com.myvoice.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ThoughtDao {

    @Upsert
    suspend fun upsert(thought: Thought)

    @Query("SELECT * FROM thoughts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Thought>>

    @Query("SELECT * FROM thoughts ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<Thought>

    @Query("SELECT * FROM thoughts WHERE id = :id")
    suspend fun byId(id: String): Thought?

    @Query("SELECT * FROM thoughts WHERE id = :id")
    fun observeById(id: String): Flow<Thought?>

    @Query(
        "SELECT * FROM thoughts WHERE title LIKE :q OR transcript LIKE :q OR reply LIKE :q " +
            "ORDER BY createdAt DESC"
    )
    fun search(q: String): Flow<List<Thought>>

    @Query("UPDATE thoughts SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("DELETE FROM thoughts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM thoughts")
    suspend fun clearAll()

    @Query("SELECT * FROM thoughts WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun unsynced(): List<Thought>

    @Query("UPDATE thoughts SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("SELECT COUNT(*) FROM thoughts")
    suspend fun count(): Int
}
