package bookrec.repository

import bookrec.model.Book
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BookRepository : JpaRepository<Book, Long> {
    fun findByGenre(genre: String): List<Book>
    @Query("SELECT b FROM Book b WHERE b.averageRating > :minRating")
    fun findTopRated(minRating: Double): List<Book>
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    fun findByTitleContainingIgnoreCase(@Param("title") title: String): List<Book>
}