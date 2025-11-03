package bookrec.controller

import bookrec.model.User
import bookrec.model.UserContext
import bookrec.service.CsvProcessingService
import bookrec.service.LlmRecommendationStrategy
import bookrec.service.RatingService
import bookrec.service.RecommendationService
import bookrec.service.UserService
import com.opencsv.exceptions.CsvValidationException
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
    private val csvProcessingService: CsvProcessingService,
    private val recommendationStrategy: LlmRecommendationStrategy,
    private val recommendationService: RecommendationService,
    private val ratingService: RatingService
) {
    private val logger = LoggerFactory.getLogger(UserController::class.java)

    // --- CREATE USER (only allowed by admins) ---
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    fun createUser(@RequestBody user: User): ResponseEntity<User> {
        logger.info("Creating new user: {}", user.username)
        val createdUser = userService.createUser(user)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser)
    }

    // --- READ ALL USERS (admins only) ---
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    fun getAllUsers(): ResponseEntity<List<User>> {
        logger.info("Fetching all users")
        return ResponseEntity.ok(userService.getAllUsers())
    }

    // --- READ ONE (authenticated user only for their own data) ---
    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal principal: User): ResponseEntity<User> {
        val user = userService.getUserById(principal.id)
        return if (user != null) ResponseEntity.ok(user)
        else ResponseEntity.notFound().build()
    }

    // --- UPDATE PROFILE (only self) ---
    @PostMapping("/update_profile")
    fun updateProfile(
        @RequestBody updatedUser: User,
        @AuthenticationPrincipal principal: User
    ): ResponseEntity<User> {
        if (updatedUser.id != principal.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val user = userService.updateProfile(updatedUser)
        return if (user != null) ResponseEntity.ok(user)
        else ResponseEntity.notFound().build()
    }

    // --- DELETE (only self) ---
    @Transactional
    @DeleteMapping("/me")
    fun deleteCurrentUser(@AuthenticationPrincipal principal: User): ResponseEntity<Void> {

        try {
            // delete all recommendations
            val recDeleted = recommendationService.deleteRecommendationsByUserId(principal.id)
            logger.debug("Deleted $recDeleted recommendations for user id=${principal.id}")

            // delete all ratings
            val ratingsDeleted = ratingService.deleteRatingByUserId(principal.id)
            logger.debug("Deleted $ratingsDeleted ratings for user id=${principal.id}")

            // delete the user itself
            return if (userService.deleteUser(principal.id)) {
                ResponseEntity.noContent().build()
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (ex: Exception) {
            logger.error("Failed to delete user and related data for user id=${principal.id}", ex)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    // --- IMPORT DATA / BATCH ADD (admin only) ---
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import_data")
    fun importData(@RequestBody users: List<User>): ResponseEntity<List<User>> {
        logger.info("Importing batch of {} users", users.size)
        val createdUsers = userService.importUsers(users)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUsers)
    }

    // --- GENERATE USER CONTEXT (only self) ---
    @PostMapping("/generate_user_context")
    fun generateUserContext(
        @AuthenticationPrincipal principal: User,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<UserContext?> {

        if (file.isEmpty || file.contentType != "text/csv") {
            logger.warn("Bad request: file empty or not CSV. Actual type: ${file.contentType}")
            return ResponseEntity.badRequest().build()
        }

        return try {
            val summary = csvProcessingService.processCsv(file)
            val userContext = recommendationStrategy.generateUserProfile(principal.id, summary)
            userContext?.let {
                it.id = principal.id
                userService.saveUserContext(principal.id, userContext)
                logger.info("User context updated: {}", userContext)
            }
            ResponseEntity.ok(userContext)
        } catch (e: IOException) {
            logger.error(e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        } catch (e: CsvValidationException) {
            logger.error(e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        } catch (e: IllegalArgumentException) {
            logger.error(e.message, e)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null)
        }
    }

    // --- GET USER CONTEXT
    @GetMapping("/get_user_context")
    fun getUserContext(
        @AuthenticationPrincipal principal: User
    ): ResponseEntity<UserContext?> {

        return try {
            val userContext = userService.findUserContextById(principal.id)

            ResponseEntity.ok(userContext)
        } catch (e: Exception) {
            logger.error(e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}