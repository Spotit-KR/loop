package kr.io.team.loop.auth.domain.model

import kr.io.team.loop.common.domain.exception.InvalidInputException

@JvmInline
value class Nickname(
    val value: String,
) {
    init {
        if (value.isBlank()) throw InvalidInputException("Nickname must not be blank")
        if (value.length > 30) throw InvalidInputException("Nickname must not exceed 30 characters")
    }
}
