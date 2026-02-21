package kr.io.team.loop.category.application.dto

import kr.io.team.loop.category.domain.model.Category
import kr.io.team.loop.category.domain.model.CategoryColor
import kr.io.team.loop.category.domain.model.CategoryName
import kr.io.team.loop.category.domain.model.SortOrder
import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId
import java.time.LocalDateTime

data class CategoryDto(
    val id: CategoryId,
    val memberId: MemberId,
    val name: CategoryName,
    val color: CategoryColor,
    val sortOrder: SortOrder,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(category: Category) =
            CategoryDto(
                id = category.id,
                memberId = category.memberId,
                name = category.name,
                color = category.color,
                sortOrder = category.sortOrder,
                createdAt = category.createdAt,
                updatedAt = category.updatedAt,
            )
    }
}
