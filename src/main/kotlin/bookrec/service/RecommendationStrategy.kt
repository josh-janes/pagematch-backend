package bookrec.service

import bookrec.model.Recommendation
import bookrec.model.RequestContext
import bookrec.model.UserContext
import org.springframework.stereotype.Service

interface RecommendationStrategy {
    fun generateRecommendations(userId: Long, requestContext: RequestContext): List<Recommendation>
    fun generateUserProfile(userId: Long, summary: String?): UserContext?
}