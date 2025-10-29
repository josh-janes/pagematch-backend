package bookrec.model

data class RequestContext(
    val mood: String? = null,
    val timeAvailable: String? = null,
    val purpose: String? = null,
    val genres: String? = null,
    val notes: String? = null
)

fun convertToString(context: RequestContext?): String {
    return buildString {
        context?.mood?.let { appendLine("- Current Mood: $it") }
        context?.timeAvailable?.let { appendLine("- Time Available: $it") }
        context?.purpose?.let { appendLine("- Reading Purpose: $it") }
        context?.genres?.let { appendLine("- Requested Genres: $it") }
        context?.notes?.let { appendLine("- Notes: $it") }
    }
}