package bookrec.controller

import bookrec.model.User
import bookrec.model.UserContext
import bookrec.service.CsvProcessingService
import bookrec.service.LlmRecommendationStrategy
import bookrec.service.UserService
import com.opencsv.exceptions.CsvValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.IOException


@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
    private val csvProcessingService: CsvProcessingService,
    private val recommendationStrategy: LlmRecommendationStrategy
) {
    private val logger = LoggerFactory.getLogger(UserController::class.java)

    // --- CREATE ---
    @PostMapping
    fun createUser(@RequestBody user: User): ResponseEntity<User> {
        logger.info("Creating new user: {}", user.username)
        val createdUser = userService.createUser(user)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser)
    }

    // --- READ ALL ---
    @GetMapping
    fun getAllUsers(): ResponseEntity<List<User>> {
        logger.info("Fetching all users")
        val users = userService.getAllUsers()
        return ResponseEntity.ok(users)
    }

    // --- READ ONE ---
    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): ResponseEntity<User> {
        logger.info("Fetching user with id: {}", id)
        val user = userService.getUserById(id)
        return if (user != null) ResponseEntity.ok(user)
        else {
            logger.warn("User not found with id: {}", id)
            ResponseEntity.notFound().build()
        }
    }

    // --- SEARCH BY USERNAME ---
    @GetMapping("/username/{username}")
    fun getUserByUsername(@PathVariable username: String): ResponseEntity<List<User>> {
        logger.info("Fetching users with username like: {}", username)
        val users = userService.getUserByUsername(username)
        return if (users.isNotEmpty()) ResponseEntity.ok(users)
        else {
            logger.warn("No users found with username: {}", username)
            ResponseEntity.notFound().build()
        }
    }

    // --- UPDATE PROFILE ---
    @PostMapping("/update_profile")
    fun updateProfile(@RequestBody updatedUser: User): ResponseEntity<User> {
        logger.info("Updating profile for user: {}", updatedUser.username)
        val user = userService.updateProfile(updatedUser)
        return if (user != null) ResponseEntity.ok(user)
        else {
            logger.warn("User not found for update: {}", updatedUser.username)
            ResponseEntity.notFound().build()
        }
    }

    // --- IMPORT DATA / BATCH ADD ---
    @PostMapping("/import_data")
    fun importData(@RequestBody users: List<User>): ResponseEntity<List<User>> {
        logger.info("Importing batch of {} users", users.size)
        val createdUsers = userService.importUsers(users)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUsers)
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Void> {
        logger.info("Deleting user with id: {}", id)
        return if (userService.deleteUser(id)) {
            ResponseEntity.noContent().build()
        } else {
            logger.warn("User not found for deletion: {}", id)
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/generate_user_context/{id}")
    fun generateUserContext(@PathVariable id: Long, @RequestParam("file") file: MultipartFile): ResponseEntity<UserContext?> {

        // Corrected validation logic:
        // Reject if the file is empty OR if the content type is NOT 'text/csv'
        if (file.isEmpty || !"text/csv".equals(file.contentType)) {
            // You could add a log here to see why it failed, e.g.:
            // logger.warn("Bad request: file is empty or content type is not csv. Actual type: ${file.contentType}")
            return ResponseEntity.badRequest().build<UserContext?>()
        }

        try {
            val summary: String = csvProcessingService.processCsv(file)
            val userContext: UserContext? = recommendationStrategy.generateUserProfile(id, summary)
            userContext?.let {
                it.id = id
                userService.saveUserContext(id, userContext)
                logger.info("User context updated: {}", userContext)
            }
            return ResponseEntity.ok<UserContext?>(userContext)

        } catch (e: IOException) {
            logger.error(e.message, e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<UserContext?>()
        } catch (e: CsvValidationException) {
            logger.error(e.message, e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<UserContext?>()
        } catch (e: IllegalArgumentException) {
            logger.error(e.message, e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body<UserContext?>(null)
        }
    }

}