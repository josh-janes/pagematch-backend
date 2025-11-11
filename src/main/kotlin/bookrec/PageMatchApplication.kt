package bookrec

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PageMatchApplication

fun main(args: Array<String>) {

    val port = System.getenv("PORT")?.toInt() ?: 8080
    runApplication<PageMatchApplication>(*args) {
        setDefaultProperties(mapOf("server.port" to port))
    }
}