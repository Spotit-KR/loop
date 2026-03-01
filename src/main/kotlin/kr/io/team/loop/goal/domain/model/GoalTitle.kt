package kr.io.team.loop.goal.domain.model

import kr.io.team.loop.common.domain.exception.InvalidInputException

@JvmInline
value class GoalTitle(
    val value: String,
) {
    init {
        if (value.isBlank()) throw InvalidInputException("GoalTitle must not be blank")
        if (value.length > 200) throw InvalidInputException("GoalTitle must not exceed 200 characters")
    }
}
