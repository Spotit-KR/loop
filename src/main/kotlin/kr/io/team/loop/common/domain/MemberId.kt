package kr.io.team.loop.common.domain

import kr.io.team.loop.common.domain.exception.InvalidInputException

@JvmInline
value class MemberId(
    val value: Long,
) {
    init {
        if (value <= 0) throw InvalidInputException("MemberId must be positive")
    }
}
