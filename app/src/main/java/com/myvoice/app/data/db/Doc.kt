package com.myvoice.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * A generated document (notes, email, report, blog, plan…) created from a
 * thought, a live conversation, or typed text.
 */
@Entity(tableName = "documents")
data class DocEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** DocKind id: notes / email / report / blog / plan / summary / custom */
    val kind: String,
    /** Markdown content */
    val content: String,
    /** THOUGHT / CONVERSATION / TYPED */
    val source: String,
    /** Originating thought id, if any */
    val sourceId: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Dao
interface DocDao {

    @Upsert
    suspend fun upsert(doc: DocEntity)

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DocEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun byId(id: String): DocEntity?

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeById(id: String): Flow<DocEntity?>

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM documents")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun count(): Int
}

class DocRepository(private val dao: DocDao) {

    fun observeAll(): Flow<List<DocEntity>> = dao.observeAll()

    fun observeById(id: String): Flow<DocEntity?> = dao.observeById(id)

    suspend fun byId(id: String): DocEntity? = dao.byId(id)

    suspend fun save(doc: DocEntity) = dao.upsert(doc)

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun clearAll() = dao.clearAll()

    suspend fun count(): Int = dao.count()
}
