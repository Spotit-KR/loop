package kr.io.team.loop.category.domain.model

import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId

sealed interface CategoryCommand {
    data class Create(
        val memberId: MemberId,
        val name: CategoryName,
        val color: CategoryColor,
        val sortOrder: SortOrder,
    ) : CategoryCommand

    data class Update(
        val categoryId: CategoryId,
        val memberId: MemberId,
        val name: CategoryName,
        val color: CategoryColor,
        val sortOrder: SortOrder,
    ) : CategoryCommand

    data class Delete(
        val categoryId: CategoryId,
        val memberId: MemberId,
    ) : CategoryCommand
}
