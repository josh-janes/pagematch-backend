package bookrec.service

import bookrec.repository.RatingRepository
import org.springframework.stereotype.Service

@Service
class RatingService(
    private val ratingRepository: RatingRepository,
) {

    fun deleteRatingByUserId(userId: Long): Long {
        return ratingRepository.deleteByUserId(userId)
    }
}