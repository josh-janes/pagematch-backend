package bookrec.repository

import bookrec.model.Recommendation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RecommendationRepository : JpaRepository<Recommendation, Long> {
    @Query("SELECT r FROM Recommendation r WHERE r.userId = :userId")
    fun findUserRecommendationHistory(userId: Long): List<Recommendation>

    fun findByReasonNotOrderByRecommendationIdDesc(reason: String, pageable: Pageable): Page<Recommendation>

    fun deleteByUserId(userId: Long): Long  // Returns number of rows deleted
}
