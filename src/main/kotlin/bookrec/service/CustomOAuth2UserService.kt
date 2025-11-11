package bookrec.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService(
    private val authService: AuthService
) : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oauth2User = super.loadUser(userRequest)

        val provider = userRequest.clientRegistration.registrationId // "google"
        val oauthId = oauth2User.getAttribute<String>("sub") ?: throw IllegalStateException("Sub not found")
        val email = oauth2User.getAttribute<String>("email") ?: throw IllegalStateException("Email not found")
        val name = oauth2User.getAttribute<String>("name") ?: email.substringBefore("@")

        // Process OAuth user (create or update)
        val user = authService.processOAuthUser(provider, oauthId, email, name)

        // Return custom user principal
        return DefaultOAuth2User(
            oauth2User.authorities,
            oauth2User.attributes,
            "sub"
        )
    }
}

@Component
class OAuth2SuccessHandler(
    private val authService: AuthService
) : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oauth2User = authentication.principal as OAuth2User
        val email = oauth2User.getAttribute<String>("email")

        // You can generate JWT token here if needed
        // val token = jwtService.generateToken(email)

        // Redirect to frontend with success
        val redirectUrl = "http://localhost:3000/oauth-success"
        targetUrlParameter = redirectUrl

        super.onAuthenticationSuccess(request, response, authentication)
    }
}