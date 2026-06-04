package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BacklogCardDao {
    @Query("SELECT * FROM backlog_cards ORDER BY timestamp ASC")
    fun getAllCards(): Flow<List<BacklogCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: BacklogCard): Long

    @Update
    suspend fun updateCard(card: BacklogCard)

    @Query("DELETE FROM backlog_cards WHERE id = :id")
    suspend fun deleteCardById(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<BacklogCard>)
}
