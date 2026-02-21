package kr.io.team.loop.category.presentation.response

import kr.io.team.loop.category.application.dto.CategoryDto

data class CategoryResponse(
    val id: Long,
    val memberId: Long,
    val name: String,
    val color: String,
    val sortOrder: Int,
    val createdAt: String,
    val updatedAt: String,
) {
    companion object {
        fun from(dto: CategoryDto) =
            CategoryResponse(
                id = dto.id.value,
                memberId = dto.memberId.value,
                name = dto.name.value,
                color = dto.color.value,
                sortOrder = dto.sortOrder.value,
                createdAt = dto.createdAt.toString(),
                updatedAt = dto.updatedAt.toString(),
            )
    }
}
