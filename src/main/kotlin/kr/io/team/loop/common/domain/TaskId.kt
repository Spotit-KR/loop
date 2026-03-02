package kr.io.team.loop.common.domain

import kr.io.team.loop.common.domain.exception.InvalidInputException

@JvmInline
value class TaskId(
    val value: Long,
) {
    init {
        if (value <= 0) throw InvalidInputException("TaskId must be positive")
    }
}
