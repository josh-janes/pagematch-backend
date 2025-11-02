package bookrec.repository

import bookrec.model.Book
import bookrec.model.Recommendation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RecommendationRepository : JpaRepository<Recommendation, Long> {
    @Query("SELECT r FROM Recommendation r WHERE r.userId = :userId")
    fun findUserRecommendationHistory(userId: Long): List<Recommendation>


}
