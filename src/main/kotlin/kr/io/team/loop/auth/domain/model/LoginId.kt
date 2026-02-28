package kr.io.team.loop.auth.domain.model

import kr.io.team.loop.common.domain.exception.InvalidInputException

@JvmInline
value class LoginId(
    val value: String,
) {
    init {
        if (value.isBlank()) throw InvalidInputException("LoginId must not be blank")
        if (value.length > 50) throw InvalidInputException("LoginId must not exceed 50 characters")
    }
}
