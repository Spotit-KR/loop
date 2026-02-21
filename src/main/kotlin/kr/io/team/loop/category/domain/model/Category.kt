package kr.io.team.loop.category.domain.model

import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId
import java.time.LocalDateTime

data class Category(
    val id: CategoryId,
    val memberId: MemberId,
    val name: CategoryName,
    val color: CategoryColor,
    val sortOrder: SortOrder,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
