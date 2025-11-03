package bookrec.persistence

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.security.crypto.encrypt.TextEncryptor

// Class designed for encrypting sensitive data before writing to database

@Converter
class CryptoConverter : AttributeConverter<String, String> {

    companion object {
        // A static holder to give us access to the Spring-managed TextEncryptor
        lateinit var textEncryptor: TextEncryptor
    }

    /**
     * This method is called by JPA before persisting the entity.
     * It takes the plaintext email and returns the encrypted version to be stored in the database.
     */
    override fun convertToDatabaseColumn(attribute: String?): String? {
        // Only encrypt non-null, non-blank values
        return if (attribute.isNullOrBlank()) {
            attribute
        } else {
            textEncryptor.encrypt(attribute)
        }
    }

    /**
     * This method is called by JPA after fetching the entity from the database.
     * It takes the encrypted email from the database and returns the decrypted, plaintext version.
     */
    override fun convertToEntityAttribute(dbData: String?): String? {
        // Only decrypt non-null, non-blank values
        return if (dbData.isNullOrBlank()) {
            dbData
        } else {
            try {
                textEncryptor.decrypt(dbData)
            } catch (e: Exception) {
                // Handle decryption failure (e.g., log a warning, return a placeholder, or throw)
                // This might happen if the key changes or data is corrupted.
                // Returning the raw data might be a security risk, so handle with care.
                // For this example, we'll log and return null.
                // Consider a more robust error handling strategy for production.
                // logger.error("Failed to decrypt database value", e)
                null
            }
        }
    }
}