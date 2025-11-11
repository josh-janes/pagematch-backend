package bookrec.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "recommendations")
data class Recommendation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var recommendationId: Long?,
    var bookId: Long,
    var userId: Long,
    val title: String,
    val author: String,
    var image_url: String,
    @Column(columnDefinition = "TEXT")
    val reason: String // LLM-generated explanation
)

fun convertToString(value: Recommendation?): String {
    return buildString {
        append("Title: ", value?.title)
        append("Reason: ", value?.reason)
    }
}