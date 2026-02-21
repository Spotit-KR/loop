package kr.io.team.loop.category.domain.model

@JvmInline
value class CategoryName(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "CategoryName must not be blank" }
        require(value.length <= 50) { "CategoryName must not exceed 50 characters" }
    }
}
