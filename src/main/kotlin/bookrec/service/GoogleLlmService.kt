package bookrec.service

import bookrec.model.Recommendation
import bookrec.model.RequestContext
import bookrec.model.UserContext
import bookrec.model.UserReadingHabits
import bookrec.prompts.GenerateUserContextPrompt
import bookrec.prompts.RequestRecommendationPrompt
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service


@Service
class GoogleLlmService(
    private val bookService: BookService,
    private val chatModel: VertexAiGeminiChatModel
) {
    fun generateUserContext(summary: String?, existingContext: UserContext): UserContext? {
        val prompt = GenerateUserContextPrompt.generateUserContextPrompt(summary, existingContext)
        val userReadingHabits = ChatClient.create(chatModel)
            .prompt()
            .user(prompt)
            .call()
            .entity(UserReadingHabits::class.java)

        userReadingHabits?.let {
            return UserContext(
                existingContext.id,
                it.favoriteGenres,
                existingContext.favoriteBooks,
                it.preferredAuthors,
                it.readingLevel,
                it.readerSummary
            )
        }
        return existingContext
    }

    fun generateRecommendation(context: UserContext, requestContext: RequestContext): List<Recommendation> {

        val prompt = RequestRecommendationPrompt.getRequestRecommendationPrompt(context, requestContext, null, null)
        val recommendations = ChatClient.create(chatModel)
            .prompt()
            .user(prompt)
            .call()
            .entity(object : ParameterizedTypeReference<List<Recommendation>>() {})

        // change id to the id of a book in our system
        if (recommendations != null) {
            for (recommendation in recommendations) {
                recommendation.image_url = ""
                recommendation.userId = context.id
                recommendation.bookId = -1
                val books = bookService.getBooksByTitleAndAuthor(recommendation.title, recommendation.author)
                books.firstOrNull()?.let { book ->
                    recommendation.bookId = book.id!!
                    recommendation.image_url = book.coverImageUrl!!
                }
            }
            return recommendations
        }
        else {
            return emptyList()
        }
    }
}
