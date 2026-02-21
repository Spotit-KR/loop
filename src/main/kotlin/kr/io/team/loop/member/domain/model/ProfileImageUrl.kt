package kr.io.team.loop.member.domain.model

@JvmInline
value class ProfileImageUrl(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "ProfileImageUrl must not be blank" }
        require(value.length <= 500) { "ProfileImageUrl must not exceed 500 characters" }
    }
}
