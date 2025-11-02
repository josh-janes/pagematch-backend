package bookrec.controller

import bookrec.model.Recommendation
import bookrec.model.RequestContext
import bookrec.model.User
import bookrec.service.RecommendationService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/recommendations")
class RecommendationController(
    private val recommendationService: RecommendationService
) {
    private val logger = LoggerFactory.getLogger(RecommendationController::class.java)

    @PostMapping("/{userId}")
    fun generateRecommendations(
        @AuthenticationPrincipal principal: User,
        @RequestBody requestContext: RequestContext
    ): ResponseEntity<List<Recommendation>> {
        val userId = principal.id
        logger.info("Fetching recommendations for authenticated userId=$userId")
        return ResponseEntity.ok(recommendationService.generateRecommendations(userId, requestContext))

    }

    @GetMapping
    fun getRecommendations(
        @AuthenticationPrincipal principal: User,
    ): ResponseEntity<List<Recommendation>> {
        val userId = principal.id
        logger.info("Fetching recommendation with id=$userId")
        val recommendations = recommendationService.getUserRecommendationHistory(userId)
        return ResponseEntity.ok(recommendations)
    }


    @GetMapping("/{id}/details")
    fun getRecommendationById(@PathVariable id: Long): ResponseEntity<Recommendation> {
        logger.info("Fetching recommendation with id=$id")
        val recommendation = recommendationService.getRecommendationById(id)
        return if (recommendation != null) ResponseEntity.ok(recommendation)
        else ResponseEntity.notFound().build()
    }

    @PostMapping
    fun addRecommendation(
        @AuthenticationPrincipal principal: User,
        @RequestBody recommendation: Recommendation
    ): ResponseEntity<Recommendation> {
        val recWithUser = recommendation.copy(userId = principal.id)
        logger.info("Adding new recommendation for bookId=${recWithUser.bookId}")
        return ResponseEntity.ok(recommendationService.addRecommendation(recWithUser))
    }

    @PostMapping("/batch")
    fun addRecommendationsBatch(
        @AuthenticationPrincipal principal: User,
        @RequestBody recommendations: List<Recommendation>
    ): ResponseEntity<List<Recommendation>> {
        val recsWithUser = recommendations.map { it.copy(userId = principal.id) }
        logger.info("Batch adding ${recsWithUser.size} recommendations for user ${principal.id}")
        return ResponseEntity.ok(recommendationService.addRecommendationsBatch(recsWithUser))
    }

    @PutMapping("/{id}")
    fun updateRecommendation(
        @PathVariable id: Long,
        @RequestBody updated: Recommendation,
        @AuthenticationPrincipal principal: User
    ): ResponseEntity<Recommendation> {
        val existing = recommendationService.getRecommendationById(id)
        if (existing == null || existing.userId != principal.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val result = recommendationService.updateRecommendation(id, updated.copy(userId = principal.id))
        return ResponseEntity.ok(result!!)
    }

    @DeleteMapping("/{id}")
    fun deleteRecommendation(
        @PathVariable id: Long,
        @AuthenticationPrincipal principal: User
    ): ResponseEntity<Void> {
        val existing = recommendationService.getRecommendationById(id)
        if (existing == null || existing.userId != principal.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        recommendationService.deleteRecommendation(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/rate_recommendation")
    fun rateRecommendation(
        @RequestBody recommendation: Recommendation,
        @RequestParam rating: Int,
        @AuthenticationPrincipal principal: User
    ): ResponseEntity<String> {
        if (recommendation.userId != principal.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot rate another user's recommendation")
        }
        recommendationService.rateRecommendation(recommendation)
        return ResponseEntity.ok("Rating submitted successfully")
    }

    @GetMapping("/popular")
    fun getPopularRecommendations(): ResponseEntity<List<Recommendation>> {
        logger.info("Fetching popular recommendations")
        return ResponseEntity.ok(recommendationService.getPopularRecommendations())
    }

    @GetMapping("/recent")
    fun getRecentRecommendations(): ResponseEntity<List<Recommendation>> {
        logger.info("Fetching recommendations")
        return ResponseEntity.ok(recommendationService.getRecentRecommendations())
    }
}
