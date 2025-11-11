package bookrec

import bookrec.service.CustomOAuth2UserService
import bookrec.service.JwtAuthenticationFilter
import bookrec.service.UserService
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val userDetailsService: UserService,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationProvider(): DaoAuthenticationProvider {
        val provider = DaoAuthenticationProvider()
        provider.setUserDetailsService(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder())
        return provider
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    @Bean
    fun filterChain(http: HttpSecurity, customOAuth2UserService: CustomOAuth2UserService): SecurityFilterChain {
        http
            .securityMatcher("/**")
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf{ it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/health/**",
                        "/api/health",
                        "api/auth/register",
                        "/api/auth/login",
                        "/api/auth/status"
                    ).permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
//                    .requestMatchers("/", "/error", "/webjars/**").permitAll()

//                    .requestMatchers("/api/auth/**", "/oauth2/**", "/login/oauth2/**", "/login/oauth2/code/**").permitAll()
                    .anyRequest().authenticated()
            }
//            .oauth2Login { oauth ->
//                oauth
//                    .loginPage("/login")
//                    .defaultSuccessUrl("http://localhost:3000/oauth-success", true)
//                    .failureUrl("http://localhost:3000/login?error=oauth")
//                    .userInfoEndpoint { it.userService(customOAuth2UserService) }
//            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { logout ->
                logout
                    .logoutUrl("/api/auth/logout")
                    .logoutSuccessUrl("/login")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
            }
            .authenticationProvider(authenticationProvider())

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()

        config.allowedOrigins = listOf(
            "http://localhost:3000",
            "http://localhost:5173",
            "https://pagematch.ca",
            "https://www.pagematch.ca"
        )
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true
        config.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
