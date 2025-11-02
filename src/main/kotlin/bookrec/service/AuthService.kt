package bookrec.service

import bookrec.model.AuthResponse
import bookrec.model.RegisterRequest
import bookrec.model.LoginRequest
import bookrec.model.User
import bookrec.repository.UserRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager
) {

    fun register(request: RegisterRequest): AuthResponse {
        // Validate username and email don't already exist
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username already exists")
        }
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already exists")
        }

        // Create new user with encoded password
        val user = User(
            username = request.username,
            email = request.email,
            password = passwordEncoder.encode(request.password),
            roles = "ROLE_USER"
        )

        val savedUser = userRepository.save(user)

        return AuthResponse(
            id = savedUser.id,
            username = savedUser.username,
            email = savedUser.email,
            avatarUrl = null,
            message = "User registered successfully"
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        // Authenticate the user
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                request.username,
                request.password
            )
        )

        // Set authentication in security context
        SecurityContextHolder.getContext().authentication = authentication

        // Get the authenticated user
        val user = authentication.principal as User

        return AuthResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            avatarUrl = null,
            message = "Login successful"
        )
    }

    // Placeholder for OAuth login - to be implemented
    fun loginWithOAuth(provider: String): AuthResponse {
        // This will redirect to OAuth provider
        // Implementation depends on Spring Security OAuth2 client setup
        throw NotImplementedError("OAuth login to be implemented")
    }

    // Helper method to create or update OAuth user
    fun processOAuthUser(provider: String, oauthId: String, email: String, username: String): User {
        // Check if user exists with this OAuth provider and ID
        return userRepository.findByOauthProviderAndOauthId(provider, oauthId)
            .orElseGet {
                // Create new OAuth user
                val newUser = User(
                    username = username,
                    email = email,
                    oauthProvider = provider,
                    oauthId = oauthId,
                    roles = "ROLE_USER",
                    password = null // No password for OAuth users
                )
                userRepository.save(newUser)
            }
    }
}