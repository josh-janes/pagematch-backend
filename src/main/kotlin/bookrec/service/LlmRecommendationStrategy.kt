package bookrec.service

import bookrec.model.Rating
import bookrec.model.Recommendation
import org.springframework.stereotype.Component

@Component
class LlmRecommendationStrategy(
    private val llmClient: LlmClient // Injected Spring AI client
) : RecommendationStrategy {
    override fun generateRecommendations(userRatings: List<Rating>): List<Recommendation> {
        val input = buildLlmInput(userRatings)
        val llmOutput = llmClient.generate(input) // Call Hugging Face DistilBERT
        return parseLlmOutput(llmOutput, userRatings)
    }

    private fun buildLlmInput(ratings: List<Rating>): String {
        val genres = ratings.map { it.book.genre }.distinct()
        return "User likes genres: ${genres.joinToString()}. Recommend books."
    }

    private fun parseLlmOutput(output: String, ratings: List<Rating>): List<Recommendation> {
        // Mock parsing; replace with actual LLM response parsing
        val book = ratings.first().book // Simplified example
        return listOf(Recommendation(1, book.id, book.title, "Matches your ${book.genre} preference"))
    }
}