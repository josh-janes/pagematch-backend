package bookrec.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "recommendations")
data class Recommendation(
    @Id
    var recommendationId: Long,
    var bookId: Long,
    val userId: Long,
    val title: String,
    val author: String,
    var image_url: String,
    val reason: String // LLM-generated explanation
)

fun convertToString(value: Recommendation?): String {
    return buildString {
        append("Title: ", value?.title)
        append("Reason: ", value?.reason)
    }
}