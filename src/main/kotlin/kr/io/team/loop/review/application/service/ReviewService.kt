package kr.io.team.loop.review.application.service

import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.exception.DuplicateEntityException
import kr.io.team.loop.review.application.dto.ReviewStatsDto
import kr.io.team.loop.review.domain.model.Review
import kr.io.team.loop.review.domain.model.ReviewCommand
import kr.io.team.loop.review.domain.model.ReviewQuery
import kr.io.team.loop.review.domain.repository.ReviewRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
) {
    @Transactional
    fun create(command: ReviewCommand.Create): Review =
        try {
            reviewRepository.save(command)
        } catch (e: DataIntegrityViolationException) {
            throw DuplicateEntityException("Review already exists for this period")
        }

    @Transactional(readOnly = true)
    fun findAll(query: ReviewQuery): List<Review> {
        val reviews = reviewRepository.findAll(query.copy(stepType = null))
        val stepType = query.stepType ?: return reviews
        return reviews.filter { it.containsStepType(stepType) }
    }

    @Transactional(readOnly = true)
    fun getStats(
        memberId: MemberId,
        today: LocalDate,
    ): ReviewStatsDto {
        val totalCount = reviewRepository.countByMemberId(memberId)
        val reviews = reviewRepository.findAll(ReviewQuery(memberId = memberId))
        val consecutiveDays = calculateConsecutiveDays(reviews, today)
        return ReviewStatsDto(
            totalCount = totalCount,
            consecutiveDays = consecutiveDays,
        )
    }

    private fun calculateConsecutiveDays(
        reviews: List<Review>,
        today: LocalDate,
    ): Int {
        val reviewDates = reviews.map { it.startDate }.toSet()
        var count = 0
        var current = today
        while (current in reviewDates) {
            count++
            current = current.minus(1, kotlinx.datetime.DateTimeUnit.DAY)
        }
        return count
    }
}
