package kr.io.team.loop.review.domain.model

import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.exception.InvalidInputException

@JvmInline
value class PeriodKey(
    val value: String,
) {
    init {
        if (value.isBlank()) throw InvalidInputException("PeriodKey must not be blank")
    }

    companion object {
        fun daily(date: LocalDate): PeriodKey = PeriodKey("DAILY:$date")
    }
}
