package kr.io.team.loop.category.domain.model

@JvmInline
value class CategoryColor(
    val value: String,
) {
    init {
        require(value.matches(Regex("^#[0-9a-fA-F]{6}$"))) {
            "CategoryColor must be in #RRGGBB format"
        }
    }
}
