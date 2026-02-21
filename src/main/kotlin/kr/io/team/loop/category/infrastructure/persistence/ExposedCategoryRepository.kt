package kr.io.team.loop.category.infrastructure.persistence

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import kr.io.team.loop.category.domain.model.Category
import kr.io.team.loop.category.domain.model.CategoryColor
import kr.io.team.loop.category.domain.model.CategoryCommand
import kr.io.team.loop.category.domain.model.CategoryName
import kr.io.team.loop.category.domain.model.CategoryQuery
import kr.io.team.loop.category.domain.model.SortOrder
import kr.io.team.loop.category.domain.repository.CategoryRepository
import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.infrastructure.persistence.CategoriesTable
import kr.io.team.loop.common.infrastructure.persistence.TasksTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

@Repository
class ExposedCategoryRepository : CategoryRepository {
    override fun save(command: CategoryCommand): Category =
        when (command) {
            is CategoryCommand.Create -> insert(command)
            is CategoryCommand.Update -> update(command)
            is CategoryCommand.Delete -> throw IllegalArgumentException("Use delete() for Delete command")
        }

    private fun insert(command: CategoryCommand.Create): Category {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val id =
            CategoriesTable.insertAndGetId {
                it[memberId] = command.memberId.value
                it[name] = command.name.value
                it[color] = command.color.value
                it[sortOrder] = command.sortOrder.value
                it[createdAt] = now
                it[updatedAt] = now
            }
        return findById(CategoryId(id.value))
            ?: throw IllegalStateException("Failed to retrieve inserted category")
    }

    private fun update(command: CategoryCommand.Update): Category {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val entityId = EntityID(command.categoryId.value, CategoriesTable)
        CategoriesTable.update(where = { CategoriesTable.id eq entityId }) {
            it[name] = command.name.value
            it[color] = command.color.value
            it[sortOrder] = command.sortOrder.value
            it[updatedAt] = now
        }
        return findById(command.categoryId)
            ?: throw NoSuchElementException("Category not found: ${command.categoryId.value}")
    }

    private fun findById(id: CategoryId): Category? {
        val entityId = EntityID(id.value, CategoriesTable)
        return CategoriesTable
            .selectAll()
            .where { CategoriesTable.id eq entityId }
            .singleOrNull()
            ?.toCategory()
    }

    override fun findAll(query: CategoryQuery): List<Category> {
        var dbQuery = CategoriesTable.selectAll()
        query.memberId?.let { memberId ->
            dbQuery = dbQuery.andWhere { CategoriesTable.memberId eq memberId.value }
        }
        query.categoryId?.let { categoryId ->
            val entityId = EntityID(categoryId.value, CategoriesTable)
            dbQuery = dbQuery.andWhere { CategoriesTable.id eq entityId }
        }
        query.name?.let { name ->
            dbQuery = dbQuery.andWhere { CategoriesTable.name eq name.value }
        }
        return dbQuery.map { it.toCategory() }
    }

    override fun delete(command: CategoryCommand.Delete): Category {
        val hasTasks =
            TasksTable
                .selectAll()
                .where { TasksTable.categoryId eq command.categoryId.value }
                .any()
        require(!hasTasks) { "Cannot delete category with existing tasks" }
        val category =
            findById(command.categoryId)
                ?: throw NoSuchElementException("Category not found: ${command.categoryId.value}")
        val entityId = EntityID(command.categoryId.value, CategoriesTable)
        CategoriesTable.deleteWhere { id eq entityId }
        return category
    }

    private fun ResultRow.toCategory(): Category =
        Category(
            id = CategoryId(this[CategoriesTable.id].value),
            memberId = MemberId(this[CategoriesTable.memberId]),
            name = CategoryName(this[CategoriesTable.name]),
            color = CategoryColor(this[CategoriesTable.color]),
            sortOrder = SortOrder(this[CategoriesTable.sortOrder]),
            createdAt = this[CategoriesTable.createdAt].toJavaLocalDateTime(),
            updatedAt = this[CategoriesTable.updatedAt].toJavaLocalDateTime(),
        )
}
