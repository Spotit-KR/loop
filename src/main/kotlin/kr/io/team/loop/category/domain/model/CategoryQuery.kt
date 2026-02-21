package kr.io.team.loop.category.domain.model

import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId

data class CategoryQuery(
    val memberId: MemberId? = null,
    val categoryId: CategoryId? = null,
    val name: CategoryName? = null,
)
