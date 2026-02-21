package kr.io.team.loop.common.domain

@JvmInline
value class Email(
    val value: String,
) {
    init {
        require(value.matches(EMAIL_REGEX)) { "Invalid email format" }
        require(value.length <= 255) { "Email must not exceed 255 characters" }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
