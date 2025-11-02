package bookrec

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class CorsAwareAuthenticationEntryPoint : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        response.addHeader("Access-Control-Allow-Origin", "http://localhost:3000")
        response.addHeader("Access-Control-Allow-Credentials", "true")

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.message)
    }
}