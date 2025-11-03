package bookrec


import bookrec.persistence.CryptoConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.encrypt.TextEncryptor
import org.springframework.security.crypto.encrypt.Encryptors

@Configuration
class EncryptionConfig(
    // 1. Inject properties directly from application.properties into the constructor.
    // This is cleaner than using @Value on individual fields.
    @Value("\${encryption.password}") private val password: String,
    @Value("\${encryption.salt}") private val salt: String
) {

    @Bean
    fun textEncryptor(): TextEncryptor {
        // Perform validation first
        if (password.isBlank() || salt.isBlank()) {
            throw IllegalStateException("encryption.password and encryption.salt must not be blank")
        }

        // 2. Create the TextEncryptor instance
        val encryptor = Encryptors.text(password, salt)

        // 3. Initialize the static field in your converter.
        // This is the key step that replaces the EncryptionInitializer.
        // It happens safely when Spring constructs this bean.
        CryptoConverter.textEncryptor = encryptor

        // Finally, return the instance so Spring can manage it as a bean
        // and inject it elsewhere if needed.
        return encryptor
    }
}