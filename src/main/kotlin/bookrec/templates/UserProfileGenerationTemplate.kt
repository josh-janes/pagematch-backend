package bookrec.templates

import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.PromptTemplateActions
import org.springframework.ai.chat.prompt.PromptTemplateChatActions

class UserProfileGenerationTemplate : PromptTemplateActions, PromptTemplateChatActions {
    override fun create(): Prompt? {
        TODO("Not yet implemented")
    }

    override fun create(modelOptions: ChatOptions?): Prompt? {
        TODO("Not yet implemented")
    }

    override fun create(model: Map<String?, Any?>?): Prompt? {
        TODO("Not yet implemented")
    }

    override fun create(
        model: Map<String?, Any?>?,
        modelOptions: ChatOptions?
    ): Prompt? {
        TODO("Not yet implemented")
    }

    override fun render(): String? {
        TODO("Not yet implemented")
    }

    override fun render(model: Map<String?, Any?>?): String? {
        TODO("Not yet implemented")
    }

    override fun createMessages(): List<Message?>? {
        TODO("Not yet implemented")
    }

    override fun createMessages(model: Map<String?, Any?>?): List<Message?>? {
        TODO("Not yet implemented")
    }


}