package bookrec.model

import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(columnNames = ["username"])]
)
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    private val username: String,

    @Column(nullable = false)
    val email: String,

    // Password field - will be BCrypt encoded
    // Nullable to support OAuth-only users who don't have passwords
    @Column(nullable = true)
    private val password: String? = null,

    // OAuth provider (e.g., "google", "github", null for local users)
    @Column(name = "oauth_provider")
    val oauthProvider: String? = null,

    // OAuth provider user ID
    @Column(name = "oauth_id")
    val oauthId: String? = null,

    // User roles (comma-separated or use @ElementCollection for proper mapping)
    @Column(nullable = false)
    val roles: String = "ROLE_USER",

    // Account status flags
    @Column(nullable = false)
    private val enabled: Boolean = true,

    @Column(nullable = false)
    private val accountNonExpired: Boolean = true,

    @Column(nullable = false)
    private val accountNonLocked: Boolean = true,

    @Column(nullable = false)
    private val credentialsNonExpired: Boolean = true
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return roles.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { SimpleGrantedAuthority(it) }
    }

    override fun getPassword(): String? = password

    override fun getUsername(): String = username

    override fun isAccountNonExpired(): Boolean = accountNonExpired

    override fun isAccountNonLocked(): Boolean = accountNonLocked

    override fun isCredentialsNonExpired(): Boolean = credentialsNonExpired

    override fun isEnabled(): Boolean = enabled
}