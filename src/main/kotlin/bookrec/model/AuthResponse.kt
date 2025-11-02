package bookrec.model

data class AuthResponse(
    val username: String? = null,
    val message: String,
    val token: String? = null, // For future JWT implementation
    val id: Long? = null,
    val avatarUrl: String? = null,
    val email: String? = null
)