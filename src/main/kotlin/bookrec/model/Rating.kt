package bookrec.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "ratings")
data class Rating(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne
    val user: User,
    @ManyToOne
    val book: Book,
    val rating: Int // 1–5
)

fun convertToString(rating: Rating?): String {
    return buildString {
        rating?.book?.let { appendLine("Book title: " + it.title) }
        rating?.rating?.let { appendLine("Rating: " + it)}
    }
}
