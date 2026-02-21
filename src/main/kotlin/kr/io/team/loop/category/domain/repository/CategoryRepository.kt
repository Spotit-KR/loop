package kr.io.team.loop.category.domain.repository

import kr.io.team.loop.category.domain.model.Category
import kr.io.team.loop.category.domain.model.CategoryCommand
import kr.io.team.loop.category.domain.model.CategoryQuery
import kr.io.team.loop.common.domain.CategoryId

interface CategoryRepository {
    fun save(command: CategoryCommand): Category

    fun findAll(query: CategoryQuery): List<Category>

    fun delete(command: CategoryCommand.Delete): Category
}
