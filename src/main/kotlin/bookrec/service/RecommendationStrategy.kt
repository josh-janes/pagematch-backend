package bookrec.service

import bookrec.model.Rating
import bookrec.model.Recommendation

interface RecommendationStrategy {
    fun generateRecommendations(userRatings: List<Rating>): List<Recommendation>
}