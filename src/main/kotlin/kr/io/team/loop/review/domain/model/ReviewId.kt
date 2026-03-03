package kr.io.team.loop.review.domain.model

import kr.io.team.loop.common.domain.exception.InvalidInputException

@JvmInline
value class ReviewId(
    val value: Long,
) {
    init {
        if (value <= 0) throw InvalidInputException("ReviewId must be positive")
    }
}
