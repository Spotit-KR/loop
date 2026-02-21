package kr.io.team.loop.category.domain.model

@JvmInline
value class SortOrder(
    val value: Int,
) {
    init {
        require(value >= 0) { "SortOrder must be non-negative" }
    }
}
