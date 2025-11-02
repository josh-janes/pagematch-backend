package bookrec.controller

import bookrec.model.User
import bookrec.model.LoginRequest
import bookrec.model.AuthResponse
import bookrec.model.RegisterRequest
import bookrec.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val response = authService.register(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal userDetails: User): ResponseEntity<AuthResponse> {
        return ResponseEntity.ok(
            AuthResponse(
                id = userDetails.id,
                username = userDetails.username,
                email = userDetails.email,
                avatarUrl = null, // Add avatar URL field to User entity if needed
                message = "User authenticated"
            )
        )
    }

    @GetMapping("/status")
    fun checkAuthStatus(@AuthenticationPrincipal userDetails: User?): ResponseEntity<Map<String, Boolean>> {
        return ResponseEntity.ok(mapOf("authenticated" to (userDetails != null)))
    }
}

// Separate controller for OAuth endpoints (dummy implementation)
@RestController
@RequestMapping("/oauth2")
class OAuth2Controller {

    @GetMapping("/authorization/google")
    fun initiateGoogleLogin(): ResponseEntity<Map<String, String>> {
        // Dummy endpoint - will redirect to actual Google OAuth when implemented
        return ResponseEntity.ok(mapOf(
            "message" to "OAuth not configured yet",
            "status" to "Please set up Google OAuth credentials"
        ))
    }

    @GetMapping("/callback")
    fun oauth2Callback(): ResponseEntity<Map<String, String>> {
        // Dummy callback endpoint
        return ResponseEntity.ok(mapOf(
            "message" to "OAuth callback - to be implemented",
            "status" to "pending"
        ))
    }
}