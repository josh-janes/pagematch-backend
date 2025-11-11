package bookrec.service

import bookrec.model.User
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.util.Date

@Component
class JwtTokenProvider {

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${jwt.expiration:86400000}") // 24 hours default
    private var jwtExpiration: Long = 86400000

    private val logger = LoggerFactory.getLogger(JwtTokenProvider::class.java)

    fun generateToken(authentication: Authentication): String {
        val user = authentication.principal as User
        val now = Date()
        val expiryDate = Date(now.time + jwtExpiration)

        return Jwts.builder()
            .setSubject(user.username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret.toByteArray())
            .compact()
    }

    fun getUsernameFromToken(token: String): String {
        val claims = Jwts.parserBuilder()
            .setSigningKey(jwtSecret.toByteArray())
            .build()
            .parseClaimsJws(token)
            .body

        return claims.subject
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(jwtSecret.toByteArray())
                .build()
                .parseClaimsJws(token)
            true
        } catch (ex: Exception) {
            logger.error("Invalid JWT token: ${ex.message}")
            false
        }
    }
}