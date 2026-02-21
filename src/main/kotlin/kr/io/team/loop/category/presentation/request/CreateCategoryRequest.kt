package kr.io.team.loop.category.presentation.request

data class CreateCategoryRequest(
    val name: String,
    val color: String,
    val sortOrder: Int,
)
