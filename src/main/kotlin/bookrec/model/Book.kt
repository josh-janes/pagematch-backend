package bookrec.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType

@Entity
data class Book(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val title: String,
    val author: String,
    val genre: String,
    val averageRating: Double,
    val synopsis: String,

    @Column(name = "cover_image_url")
    val coverImageUrl: String? = null
)