package bookrec.service

import bookrec.model.Recommendation
import bookrec.repository.RecommendationRepository
import bookrec.repository.RatingRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class RecommendationService(
    private val recommendationRepository: RecommendationRepository,
    private val ratingRepository: RatingRepository,
    private val strategy: RecommendationStrategy
) {
    private val logger = LoggerFactory.getLogger(RecommendationService::class.java)

    fun getRecommendations(userId: Long): List<Recommendation> {
        logger.debug("Generating recommendations for userId=$userId")
        val ratings = ratingRepository.findById(userId).getOrNull()
        // return strategy.generateRecommendations(ratings)
        return emptyList()
    }

    fun getRecommendationById(id: Long): Recommendation? {
        logger.debug("Fetching recommendation by id=$id")
        return recommendationRepository.findById(id).getOrNull()
    }

    fun addRecommendation(recommendation: Recommendation): Recommendation {
        logger.debug("Saving new recommendation: $recommendation")
        return recommendationRepository.save(recommendation)
    }

    fun addRecommendationsBatch(recommendations: List<Recommendation>): List<Recommendation> {
        logger.debug("Saving ${recommendations.size} recommendations in batch")
        return recommendationRepository.saveAll(recommendations)
    }

    fun updateRecommendation(id: Long, updated: Recommendation): Recommendation? {
        logger.debug("Updating recommendation id=$id")
        val existing = recommendationRepository.findById(id).getOrNull() ?: return null
        val toSave = existing.copy(
            bookId = updated.bookId,
            reason = updated.reason,
            recommendationId = updated.recommendationId,
            title = updated.title
        )
        return recommendationRepository.save(toSave)
    }

    fun deleteRecommendation(id: Long): Boolean {
        return try {
            logger.debug("Deleting recommendation id=$id")
            recommendationRepository.deleteById(id)
            true
        } catch (ex: EmptyResultDataAccessException) {
            logger.warn("Attempted to delete non-existent recommendation id=$id")
            false
        }
    }

    fun rateRecommendation(recommendation: Recommendation) {
        logger.debug("Processing rating for recommendation id=${recommendation.recommendationId}")
        // logic to update the rating or link to Rating entity
        recommendationRepository.save(recommendation)
    }

    fun getPopularRecommendations(): List<Recommendation> {
        logger.debug("Fetching top-rated/popular recommendations")
        val pageable: Pageable = PageRequest.of(0, 10)
        return recommendationRepository.findAll(pageable).content
    }
}
