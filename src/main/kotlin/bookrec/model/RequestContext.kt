package bookrec.model

data class RequestContext(
    val mood: String? = null,
    val timeAvailable: String? = null,
    val purpose: String? = null, // e.g., "leisure", "education", "gift"
    val additionalPreferences: String? = null
)