package com.myvoice.app.data.db

import kotlinx.coroutines.flow.Flow

class ThoughtRepository(private val dao: ThoughtDao) {

    fun observeAll(): Flow<List<Thought>> = dao.observeAll()

    fun observeById(id: String): Flow<Thought?> = dao.observeById(id)

    fun search(query: String): Flow<List<Thought>> = dao.search("%${query.trim()}%")

    suspend fun recent(limit: Int = 5): List<Thought> = dao.recent(limit)

    suspend fun byId(id: String): Thought? = dao.byId(id)

    suspend fun save(thought: Thought) = dao.upsert(thought)

    suspend fun setFavorite(id: String, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun clearAll() = dao.clearAll()

    suspend fun unsynced(): List<Thought> = dao.unsynced()

    suspend fun markSynced(ids: List<String>) = dao.markSynced(ids)

    suspend fun count(): Int = dao.count()
}
