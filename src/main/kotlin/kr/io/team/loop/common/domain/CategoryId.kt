package kr.io.team.loop.common.domain

@JvmInline
value class CategoryId(
    val value: Long,
) {
    init {
        require(value > 0) { "CategoryId must be positive" }
    }
}
