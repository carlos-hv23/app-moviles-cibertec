package com.example.data

import kotlinx.coroutines.flow.Flow

class BacklogRepository(private val dao: BacklogCardDao) {
    val allCards: Flow<List<BacklogCard>> = dao.getAllCards()

    suspend fun insert(card: BacklogCard): Long = dao.insertCard(card)

    suspend fun update(card: BacklogCard) = dao.updateCard(card)

    suspend fun deleteById(id: Int) = dao.deleteCardById(id)

    suspend fun insertAll(cards: List<BacklogCard>) = dao.insertCards(cards)
}
