package bookrec.controller

import bookrec.model.Recommendation
import bookrec.service.RecommendationService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/recommendations")
class RecommendationController(
    private val recommendationService: RecommendationService
) {
    private val logger = LoggerFactory.getLogger(RecommendationController::class.java)

    @GetMapping("/{userId}")
    fun getRecommendations(@PathVariable userId: Long): ResponseEntity<List<Recommendation>> {
        logger.info("Fetching recommendations for userId=$userId")
        return ResponseEntity.ok(recommendationService.getRecommendations(userId))
    }

    @GetMapping("/{id}/details")
    fun getRecommendationById(@PathVariable id: Long): ResponseEntity<Recommendation> {
        logger.info("Fetching recommendation with id=$id")
        val recommendation = recommendationService.getRecommendationById(id)
        return if (recommendation != null) ResponseEntity.ok(recommendation)
        else ResponseEntity.notFound().build()
    }

    @PostMapping
    fun addRecommendation(@RequestBody recommendation: Recommendation): ResponseEntity<Recommendation> {
        logger.info("Adding new recommendation for bookId=${recommendation.bookId}, recommendationId=${recommendation.recommendationId}")
        return ResponseEntity.ok(recommendationService.addRecommendation(recommendation))
    }

    @PostMapping("/batch")
    fun addRecommendationsBatch(@RequestBody recommendations: List<Recommendation>): ResponseEntity<List<Recommendation>> {
        logger.info("Batch adding ${recommendations.size} recommendations")
        return ResponseEntity.ok(recommendationService.addRecommendationsBatch(recommendations))
    }

    @PutMapping("/{id}")
    fun updateRecommendation(@PathVariable id: Long, @RequestBody updated: Recommendation): ResponseEntity<Recommendation> {
        logger.info("Updating recommendation id=$id")
        val result = recommendationService.updateRecommendation(id, updated)
        return if (result != null) ResponseEntity.ok(result)
        else ResponseEntity.notFound().build()
    }

    @DeleteMapping("/{id}")
    fun deleteRecommendation(@PathVariable id: Long): ResponseEntity<Void> {
        logger.info("Deleting recommendation id=$id")
        return if (recommendationService.deleteRecommendation(id)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
    }

    @PostMapping("/rate_recommendation")
    fun rateRecommendation(@RequestBody recommendation: Recommendation, rating: Int): ResponseEntity<String> {
        logger.info("Recommendation ${recommendation.recommendationId} rated ${rating}")
        recommendationService.rateRecommendation(recommendation)
        return ResponseEntity.ok("Rating submitted successfully")
    }

    @GetMapping("/popular")
    fun getPopularRecommendations(): ResponseEntity<List<Recommendation>> {
        logger.info("Fetching popular recommendations")
        return ResponseEntity.ok(recommendationService.getPopularRecommendations())
    }
}
