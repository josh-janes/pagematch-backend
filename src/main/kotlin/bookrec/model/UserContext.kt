package bookrec.model

import jakarta.persistence.*
import kotlin.text.appendLine

@Entity
@Table(
    name = "user_preferences",
)
data class UserContext(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    @Column(columnDefinition = "TEXT")
    val favoriteGenres: String = "",
    @Column(columnDefinition = "TEXT")
    val favoriteBooks: String = "",
    @Column(columnDefinition = "TEXT")
    val preferredAuthors: String = "",
    val readingLevel: String = "",
    @Column(columnDefinition = "TEXT")
    val readerSummary: String = ""
)

fun convertToString(context: UserContext?): String {
    return buildString {
        if (context?.favoriteGenres?.isNotEmpty() == true) {
            appendLine("- Favorite Genres: ${context.favoriteGenres}")
        }
        if (context?.favoriteBooks?.isNotEmpty() == true) {
            appendLine("- Favorite Books: ${context.favoriteBooks}")
        }
        if (context?.preferredAuthors?.isNotEmpty() == true) {
            appendLine("- Preferred Authors: ${context.preferredAuthors}")
        }
        appendLine("- Reading Level: ${context?.readingLevel}")
        appendLine("- ReaderSummary: ${context?.readerSummary}")

    }
}