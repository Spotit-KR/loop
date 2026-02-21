package kr.io.team.loop.common.domain

@JvmInline
value class Nickname(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Nickname must not be blank" }
        require(value.length <= 50) { "Nickname must not exceed 50 characters" }
    }
}
