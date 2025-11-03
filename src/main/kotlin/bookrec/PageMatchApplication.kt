package bookrec

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
class PageMatchApplication

fun main(args: Array<String>) {
    runApplication<PageMatchApplication>(*args)
}