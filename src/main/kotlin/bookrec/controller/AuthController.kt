package bookrec.controller

import bookrec.model.User
import bookrec.model.LoginRequest
import bookrec.model.AuthResponse
import bookrec.model.RegisterRequest
import bookrec.service.AuthService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {
    private val logger = LoggerFactory.getLogger(RecommendationController::class.java)

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        logger.info("Register request")
        val response = authService.register(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        logger.info("Login request")
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal userDetails: User): ResponseEntity<AuthResponse> {
        logger.info("Get current user")
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