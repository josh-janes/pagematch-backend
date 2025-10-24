package bookrec.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
data class Recommendation(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val recommendationId: Int,
    val bookId: Long,
    val title: String,
    val reason: String // LLM-generated explanation
)