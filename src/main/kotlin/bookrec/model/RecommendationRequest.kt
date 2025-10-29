package bookrec.model

data class RecommendationRequest(
    val userContext: UserContext,
    val requestContext: RequestContext,
    val systemPrompt: String? = null,
    val maxRecommendations: Int = 5
)