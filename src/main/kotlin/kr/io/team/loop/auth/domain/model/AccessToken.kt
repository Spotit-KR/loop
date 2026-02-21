package kr.io.team.loop.auth.domain.model

@JvmInline
value class AccessToken(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "AccessToken must not be blank" }
    }
}
