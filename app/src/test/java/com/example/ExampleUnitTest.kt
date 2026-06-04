package com.example

import org.junit.Assert.*
import org.junit.Test
import com.example.data.BacklogCard

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun backlogCardModel_properties_areCorrect() {
    val card = BacklogCard(
      id = 15,
      title = "Interactive Test Dashboard",
      columnId = 0,
      priority = "High",
      description = "Verifying the backlog card property mappings are consistent."
    )
    
    assertEquals(15, card.id)
    assertEquals("Interactive Test Dashboard", card.title)
    assertEquals(0, card.columnId)
    assertEquals("High", card.priority)
    assertEquals("Verifying the backlog card property mappings are consistent.", card.description)
  }
}
