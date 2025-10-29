package bookrec.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Lob
import jakarta.persistence.Table

@Entity
@Table(name = "books") // This ensures the class maps to the 'book' table
open class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @Column(name = "author")
    open var author: String? = null

    @Column(name = "cover_image_url")
    open var coverImageUrl: String? = null

    @Column(name = "description", columnDefinition = "TEXT")
    open var description: String? = null

    @Column(name = "genre")
    open var genre: String? = null

    @Column(name = "published")
    open var published = 0

    @Column(name = "ratings")
    open var ratings = 0

    @Column(name = "score")
    open var score = 0.0 // or BigDecimal for NUMERIC

    @Column(name = "shelvings")
    open var shelvings = 0 // --- Constructors, Getters, and Setters ---

    @Column(name = "title")
    open var title: String? = null
}

fun convertToString(books: List<Book>?): String? {
    return books?.joinToString("\n\n") { book ->
        """
            Book ID: ${book.id}
            Title: ${book.title}
            Author: ${book.author}
            Genre: ${book.genre}
            Rating: ${book.score}/5.0
            Synopsis: ${book.description}
            """.trimIndent()
    }
}
