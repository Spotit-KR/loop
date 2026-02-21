package kr.io.team.loop.category.presentation.request

data class UpdateCategoryRequest(
    val name: String,
    val color: String,
    val sortOrder: Int,
)
