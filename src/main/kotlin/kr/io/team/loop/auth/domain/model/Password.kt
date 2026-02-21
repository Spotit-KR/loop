package kr.io.team.loop.auth.domain.model

@JvmInline
value class Password(
    val value: String,
) {
    init {
        require(value.length >= 8) { "Password must be at least 8 characters" }
        require(value.length <= 100) { "Password must not exceed 100 characters" }
    }
}
