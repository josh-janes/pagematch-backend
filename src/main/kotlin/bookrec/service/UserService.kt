package bookrec.service

import bookrec.model.User
import bookrec.model.UserContext
import bookrec.repository.UserContextRepository
import bookrec.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import java.io.IOException


@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val userContextRepository: UserContextRepository
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    // --- CREATE ---
    fun createUser(user: User): User {
        logger.info("Creating user: {}", user.username)

        // Application-level check before hitting DB
        if (userRepository.existsByUsername(user.username)) {
            logger.warn("Username '{}' already exists", user.username)
            throw IllegalArgumentException("Username '${user.username}' already exists.")
        }

        return try {
            userRepository.save(user)
        } catch (ex: DataIntegrityViolationException) {
            // Fallback in case race conditions hit DB-level constraint
            logger.error("Database constraint violated for username '{}'", user.username)
            throw IllegalArgumentException("Username '${user.username}' already exists.")
        }
    }

    // --- READ ALL ---
    fun getAllUsers(): List<User> {
        logger.info("Fetching all users")
        return userRepository.findAll()
    }

    // --- READ ONE ---
    fun getUserById(id: Long): User? {
        logger.info("Fetching user with id: {}", id)
        return userRepository.findById(id).orElse(null)
    }

    // --- SEARCH BY USERNAME ---
    fun getUserByUsername(username: String): List<User> {
        logger.info("Searching for users with username containing: {}", username)
        return userRepository.findByUsernameContainingIgnoreCase(username)
    }

    // --- UPDATE PROFILE ---
    fun updateProfile(updatedUser: User): User? {
        logger.info("Updating profile for user: {}", updatedUser.username)
        val existing = userRepository.findById(updatedUser.id ?: return null)
        if (existing.isPresent) {
            val user = existing.get().copy(
                username = updatedUser.username,
                email = updatedUser.email
            )
            logger.debug("User before update: {}", existing.get())
            logger.debug("User after update: {}", user)
            return userRepository.save(user)
        }
        logger.warn("User not found with id: {}", updatedUser.id)
        return null
    }

    // --- DELETE ---
    fun deleteUser(id: Long): Boolean {
        logger.info("Deleting user with id: {}", id)
        return if (userRepository.existsById(id)) {
            userRepository.deleteById(id)
            true
        } else {
            logger.warn("User not found for deletion: {}", id)
            false
        }
    }

    // --- BATCH IMPORT ---
    fun importUsers(users: List<User>): List<User> {
        logger.info("Importing {} users", users.size)
        return userRepository.saveAll(users)
    }

    /**
     * Saves a UserContext.
     * If a context for the given user ID already exists, it updates it.
     * If it does not exist, it creates a new one.
     *
     * @param userId The ID of the User associated with this context.
     * @param contextDetails A UserContext object containing the data to save.
     * @return The saved (either created or updated) UserContext entity.
     */
    @Transactional
    fun saveUserContext(userId: Long, contextDetails: UserContext): UserContext {
        // Try to find an existing context for the user
        val existingContextOptional = userContextRepository.findById(userId)

        val contextToSave = if (existingContextOptional.isPresent) {
            // --- UPDATE PATH ---
            // A context already exists, so we update its fields
            val existingContext = existingContextOptional.get()
            existingContext.copy(
                favoriteGenres = contextDetails.favoriteGenres,
                favoriteBooks = contextDetails.favoriteBooks,
                preferredAuthors = contextDetails.preferredAuthors,
                readingLevel = contextDetails.readingLevel,
                readerSummary = contextDetails.readerSummary
            )
        } else {
            // --- CREATE PATH ---
            // No context exists, so we create a new one.
            // We must ensure the new entity has the correct ID.
            contextDetails.copy(id = userId)
        }

        // Save the entity. JPA will INSERT or UPDATE as needed.
        return userContextRepository.save(contextToSave)
    }

    /**
     * Finds a UserContext by its ID.
     * This method is safe: if no context is found for the given ID, it returns a new,
     * empty UserContext object with the requested ID, rather than throwing an exception.
     *
     * @param id The ID of the UserContext to find.
     * @return The found UserContext entity, or a new empty UserContext with the given ID if not found.
     */
    fun findUserContextById(id: Long): UserContext {
        return userContextRepository.findById(id)
            // If the Optional is empty (nothing found), return the result of this expression instead.
            .orElse(UserContext(id = id))
    }
}
