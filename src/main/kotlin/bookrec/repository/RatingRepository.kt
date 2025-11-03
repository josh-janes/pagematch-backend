package bookrec.repository

import bookrec.model.Rating
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RatingRepository : JpaRepository<Rating, Long> {
    fun getRatingByBookId(bookId: Long): List<Rating>

    fun deleteByUserId(userId: Long): Long  // Returns number of rows deleted
}