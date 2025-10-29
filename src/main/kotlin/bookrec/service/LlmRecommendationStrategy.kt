package bookrec.service

import bookrec.model.Recommendation
import bookrec.model.RequestContext
import bookrec.model.UserContext
import org.springframework.stereotype.Service

@Service
class LlmRecommendationStrategy(
    private val llmService: GoogleLlmService,
    private val userService: UserService
) : RecommendationStrategy {
    override fun generateRecommendations(userId: Long, requestContext: RequestContext): List<Recommendation> {

        val context = userService.findUserContextById(userId)
        val recommendations = llmService.generateRecommendation(context, requestContext)

        return recommendations
    }
    override fun generateUserProfile(userId: Long, summary: String?): UserContext? {

        val existingContext = userService.findUserContextById(userId)
        val newContext = llmService.generateUserContext(summary, existingContext)
        return newContext
    }
}