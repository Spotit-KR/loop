package kr.io.team.loop.auth.domain.model

@JvmInline
value class LoginId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "LoginId must not be blank" }
        require(value.length <= 50) { "LoginId must not exceed 50 characters" }
    }
}
