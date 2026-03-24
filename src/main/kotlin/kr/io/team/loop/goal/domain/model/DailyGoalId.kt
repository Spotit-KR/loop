package kr.io.team.loop.goal.domain.model

import kr.io.team.loop.common.domain.exception.InvalidInputException

@JvmInline
value class DailyGoalId(
    val value: Long,
) {
    init {
        if (value <= 0) throw InvalidInputException("DailyGoalId must be positive")
    }
}
