package bookrec.prompts

import bookrec.model.Rating
import bookrec.model.Recommendation
import bookrec.model.RequestContext
import bookrec.model.UserContext
import bookrec.model.convertToString

object RequestRecommendationPrompt {

    val MAX_RECS = 5

    fun getRequestRecommendationPrompt(
        userContext: UserContext?,
        requestContext: RequestContext?,
        recentRecommendations: List<Recommendation>?,
        recentRatings: List<Rating>?
    ): String {
        val userContextStr = convertToString(userContext)
        val requestContextStr = convertToString(requestContext)
        val recentRecommendationsStr = recentRecommendations?.joinToString { convertToString(it) }
        val recentRatingsStr = recentRatings?.joinToString { convertToString(it) }
        return """
            User Profile:
            $userContextStr
            
            Current Request:
            $requestContextStr
            
            Recent Recommendations:
            $recentRecommendationsStr
            
            Recent Ratings:
            $recentRatingsStr
            
            Please recommend one or more books, up to $MAX_RECS books that best match this user's profile and
            current request. Try not to recommend books the user is already likely to have read. Don't mention the 
            user's reading level. For each recommendation, write a compelling reason to convince the reader to read
            the book, based on their profile. Keep the tone casual. If the user requests something extremely vulgar or
            excessively controversial, return an empty list.
            
            Ensure each recommendation should have a personalized reason based on the user's profile and request context.
        """.trimIndent()
    }
}