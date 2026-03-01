package kr.io.team.loop.common.domain

import kr.io.team.loop.common.domain.exception.InvalidInputException

@JvmInline
value class GoalId(
    val value: Long,
) {
    init {
        if (value <= 0) throw InvalidInputException("GoalId must be positive")
    }
}
