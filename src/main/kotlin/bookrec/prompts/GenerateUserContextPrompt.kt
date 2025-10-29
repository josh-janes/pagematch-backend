package bookrec.prompts

import bookrec.model.UserContext

object GenerateUserContextPrompt {

    fun generateUserContextPrompt(summary: String?, existingContext: UserContext): String {

        val existingContextStr = existingContext.toString()
        return """
            User reading history:
            $summary
            
            Existing user context:
            $existingContextStr
            
            Please generate a profile of the user based on their reading habits, including things like favorite genres, 
            preferred authors, general reading level, and a brief (playful, good-natured)  summary of the user's 
            reading habits that will be presented to the user. Your response should contain the following fields.
           
            data class UserReadingHabits(
                val favoriteGenres: String = "",
                val preferredAuthors: String = "",
                val readingLevel: String = "",
                val readerSummary: String = ""
            )
        """.trimIndent()
    }
}