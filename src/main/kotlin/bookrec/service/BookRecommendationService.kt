package bookrec.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import bookrec.model.Book
import bookrec.model.Recommendation
import bookrec.model.RecommendationRequest
import bookrec.model.RequestContext
import bookrec.model.UserContext
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.stereotype.Service

@Service
class BookRecommendationService(
    private val chatModel: OpenAiChatModel,
    private val objectMapper: ObjectMapper
) {

    fun getRecommendations(request: RecommendationRequest): List<Recommendation> {
        val systemMsg = SystemMessage(request.systemPrompt ?: getDefaultSystemPrompt())
        val userMsg = UserMessage(buildPrompt(request))

        val prompt = Prompt(listOf(systemMsg, userMsg))
        val response = chatModel.call(prompt)

//        return parseRecommendations(response.result.output.content)
        return listOf(Recommendation(1, 1, "Lord of the Rings", "Tolkein is dope"))
    }

    private fun buildPrompt(request: RecommendationRequest): String {
        val userContextStr = buildUserContextString(request.userContext)
        val requestContextStr = buildRequestContextString(request.requestContext)
        val booksStr = buildBooksString(request.availableBooks)

        return """
            User Profile:
            $userContextStr
            
            Current Request:
            $requestContextStr
            
            Available Books:
            $booksStr
            
            Please recommend up to ${request.maxRecommendations} books from the available list that best match this user's profile and current needs.
            
            Return your response as a JSON array with this exact format:
            [
                {
                    "bookId": 1,
                    "title": "Book Title",
                    "reason": "Why this book is recommended"
                }
            ]
            
            Only recommend books from the provided list. Ensure each recommendation has a personalized reason based on the user's profile and request context.
        """.trimIndent()
    }

    private fun buildUserContextString(context: UserContext): String {
        return buildString {
            appendLine("- User ID: ${context.userId}")
            if (context.favoriteGenres.isNotEmpty()) {
                appendLine("- Favorite Genres: ${context.favoriteGenres.joinToString(", ")}")
            }
            if (context.readBooks.isNotEmpty()) {
                appendLine("- Previously Read Books (IDs): ${context.readBooks.joinToString(", ")}")
            }
            if (context.preferredAuthors.isNotEmpty()) {
                appendLine("- Preferred Authors: ${context.preferredAuthors.joinToString(", ")}")
            }
            context.readingLevel?.let {
                appendLine("- Reading Level: $it")
            }
        }
    }

    private fun buildRequestContextString(context: RequestContext): String {
        return buildString {
            context.mood?.let { appendLine("- Current Mood: $it") }
            context.timeAvailable?.let { appendLine("- Time Available: $it") }
            context.purpose?.let { appendLine("- Reading Purpose: $it") }
            context.additionalPreferences?.let { appendLine("- Additional Preferences: $it") }
        }
    }

    private fun buildBooksString(books: List<Book>): String {
        return books.joinToString("\n\n") { book ->
            """
            Book ID: ${book.id}
            Title: ${book.title}
            Author: ${book.author}
            Genre: ${book.genre}
            Rating: ${book.averageRating}/5.0
            Synopsis: ${book.synopsis}
            """.trimIndent()
        }
    }

    private fun getDefaultSystemPrompt(): String {
        return """
            You are an expert book recommendation assistant. Your job is to analyze user preferences, 
            reading history, and current context to provide personalized book recommendations.
            
            Consider the following when making recommendations:
            1. Match genres to user preferences
            2. Avoid books they've already read
            3. Consider their current mood and available time
            4. Suggest books with high ratings that fit their reading level
            5. Provide specific, personalized reasons for each recommendation
            
            Always respond with valid JSON in the requested format.
        """.trimIndent()
    }

    private fun parseRecommendations(response: String): List<Recommendation> {
        return try {
            // Extract JSON array from response (handles cases where LLM adds extra text)
            val jsonStart = response.indexOf('[')
            val jsonEnd = response.lastIndexOf(']') + 1

            if (jsonStart == -1 || jsonEnd == 0) {
                emptyList()
            } else {
                val jsonStr = response.substring(jsonStart, jsonEnd)

                val mapper = jacksonObjectMapper()

                val recommendations: List<Recommendation> = mapper.readValue(
                    jsonStr,
                    object : TypeReference<List<Recommendation>>() {}
                )
                return recommendations
            }
        } catch (e: Exception) {
            println("Failed to parse recommendations: ${e.message}")
            println("Response was: $response")
            emptyList()
        }
    }
}