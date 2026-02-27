package kr.io.team.loop.common.domain

@JvmInline
value class MemberId(
    val value: Long,
) {
    init {
        require(value > 0) { "MemberId must be positive" }
    }
}
