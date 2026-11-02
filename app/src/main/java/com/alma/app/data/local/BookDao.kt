package com.alma.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeById(id: Long): Flow<BookEntity?>

    /** Libros que empezaron o terminaron entre dos fechas ISO-8601, ambas incluidas. */
    @Query(
        """
        SELECT * FROM books
        WHERE startedOn BETWEEN :from AND :to OR finishedOn BETWEEN :from AND :to
        """,
    )
    fun observeWithDatesBetween(from: String, to: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books")
    suspend fun getAll(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE sourceId = :sourceId LIMIT 1")
    suspend fun findBySourceId(sourceId: String): BookEntity?

    @Query("SELECT * FROM books WHERE isbn13 = :isbn13 LIMIT 1")
    suspend fun findByIsbn(isbn13: String): BookEntity?

    @Insert
    suspend fun insert(book: BookEntity): Long

    @Insert
    suspend fun insertAll(books: List<BookEntity>)

    @Update
    suspend fun update(book: BookEntity)

    @Update
    suspend fun updateAll(books: List<BookEntity>)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Aplica una importación en una sola transacción: si algo falla, no cambia nada. */
    @Transaction
    suspend fun applyImport(toInsert: List<BookEntity>, toUpdate: List<BookEntity>) {
        insertAll(toInsert)
        updateAll(toUpdate)
    }
}
