package bookrec.controller

import bookrec.model.Recommendation
import bookrec.service.LoginService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/login")
class LoginController(
    private val loginService: LoginService
) {
    @GetMapping("/login")
    fun login(): ResponseEntity<List<Recommendation>> {
        return ResponseEntity.ok(null)
    }

}