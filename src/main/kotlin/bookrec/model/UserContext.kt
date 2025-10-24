package bookrec.model

data class UserContext(
    val userId: String,
    val favoriteGenres: List<String> = emptyList(),
    val readBooks: List<Long> = emptyList(),
    val preferredAuthors: List<String> = emptyList(),
    val readingLevel: String? = null
)