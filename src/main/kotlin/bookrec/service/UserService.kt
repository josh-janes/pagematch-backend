package bookrec.service

import bookrec.model.User
import bookrec.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository
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
}
