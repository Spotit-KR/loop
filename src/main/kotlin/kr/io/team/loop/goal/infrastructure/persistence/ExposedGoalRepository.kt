package kr.io.team.loop.goal.infrastructure.persistence

import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.goal.domain.model.Goal
import kr.io.team.loop.goal.domain.model.GoalCommand
import kr.io.team.loop.goal.domain.model.GoalQuery
import kr.io.team.loop.goal.domain.model.GoalTitle
import kr.io.team.loop.goal.domain.repository.GoalRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class ExposedGoalRepository : GoalRepository {
    override fun save(command: GoalCommand.Create): Goal {
        val now = OffsetDateTime.now()
        val row =
            GoalTable.insert {
                it[title] = command.title.value
                it[memberId] = command.memberId.value
                it[createdAt] = now
            }
        return Goal(
            id = GoalId(row[GoalTable.goalId]),
            title = command.title,
            memberId = command.memberId,
            createdAt = now.toInstant(),
            updatedAt = null,
        )
    }

    override fun update(command: GoalCommand.Update): Goal {
        val now = OffsetDateTime.now()
        GoalTable.update({ GoalTable.goalId eq command.goalId.value }) {
            it[title] = command.title.value
            it[updatedAt] = now
        }
        return findById(command.goalId)!!
    }

    override fun delete(command: GoalCommand.Delete) {
        GoalTable.deleteWhere { goalId eq command.goalId.value }
    }

    override fun findAll(query: GoalQuery): List<Goal> {
        var statement = GoalTable.selectAll()
        query.memberId?.let { statement = statement.where { GoalTable.memberId eq it.value } }
        return statement
            .orderBy(GoalTable.createdAt, SortOrder.DESC)
            .map { it.toGoal() }
    }

    override fun findById(id: GoalId): Goal? =
        GoalTable
            .selectAll()
            .where { GoalTable.goalId eq id.value }
            .singleOrNull()
            ?.toGoal()

    private fun ResultRow.toGoal(): Goal =
        Goal(
            id = GoalId(this[GoalTable.goalId]),
            title = GoalTitle(this[GoalTable.title]),
            memberId = MemberId(this[GoalTable.memberId]),
            createdAt = this[GoalTable.createdAt].toInstant(),
            updatedAt = this[GoalTable.updatedAt]?.toInstant(),
        )
}
