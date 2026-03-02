package kr.io.team.loop.task.domain.model

import kr.io.team.loop.common.domain.exception.InvalidInputException

@JvmInline
value class TaskTitle(
    val value: String,
) {
    init {
        if (value.isBlank()) throw InvalidInputException("TaskTitle must not be blank")
        if (value.length > 200) throw InvalidInputException("TaskTitle must not exceed 200 characters")
    }
}
