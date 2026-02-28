package kr.io.team.loop.auth.domain.model

@JvmInline
value class Nickname(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Nickname must not be blank" }
        require(value.length <= 30) { "Nickname must not exceed 30 characters" }
    }
}
