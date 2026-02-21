package kr.io.team.loop.auth.domain.model

@JvmInline
value class RefreshToken(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "RefreshToken must not be blank" }
    }
}
