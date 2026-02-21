package kr.io.team.loop.member.domain.model

@JvmInline
value class HashedPassword(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "HashedPassword must not be blank" }
    }
}
