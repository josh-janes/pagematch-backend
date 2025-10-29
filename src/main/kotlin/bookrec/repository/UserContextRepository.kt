package bookrec.repository

import bookrec.model.UserContext
import org.springframework.data.jpa.repository.JpaRepository

interface UserContextRepository : JpaRepository<UserContext, Long> {
}