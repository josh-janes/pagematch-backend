package bookrec

import com.google.cloud.vertexai.VertexAI
import org.springframework.ai.model.tool.DefaultToolCallingManager
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class VertexAIConfig {

    @Bean
    fun vertexAiGeminiChatModel(): VertexAiGeminiChatModel {
        // 1. Read the necessary project configuration from environment variables
        val projectId = System.getenv("GOOGLE_AI_PROJECT_ID")
            ?: throw IllegalStateException("GOOGLE_AI_PROJECT_ID not set")
        val location = System.getenv("GOOGLE_AI_LOCATION") ?: "us-central1"
        val modelName = System.getenv("GOOGLE_AI_MODEL") ?: "gemini-1.5-flash"

        // 2. Instantiate the VertexAI client.
        // The library will automatically find the GOOGLE_APPLICATION_CREDENTIALS
        // environment variable, read the file path from it, and use that file
        // to authenticate. No manual parsing is needed.
        val vertexAI = VertexAI(projectId, location)

        println("✅ VertexAI client initialized. ADC credentials loaded automatically.")

        // 3. Create and return the Spring AI chat model
        return VertexAiGeminiChatModel.builder()
            .vertexAI(vertexAI)
            .defaultOptions(VertexAiGeminiChatOptions.builder().model(modelName).build())
            .toolCallingManager(DefaultToolCallingManager.builder().build())
            .build()
    }
}