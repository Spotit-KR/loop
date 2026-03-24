package kr.io.team.loop.goal.infrastructure.persistence

import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.goal.domain.model.DailyGoal
import kr.io.team.loop.goal.domain.model.DailyGoalCommand
import kr.io.team.loop.goal.domain.model.DailyGoalId
import kr.io.team.loop.goal.domain.repository.DailyGoalRepository
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class ExposedDailyGoalRepository : DailyGoalRepository {
    override fun save(command: DailyGoalCommand.Add): DailyGoal {
        val now = OffsetDateTime.now()
        val row =
            DailyGoalTable.insert {
                it[goalId] = command.goalId.value
                it[memberId] = command.memberId.value
                it[date] = command.date
                it[createdAt] = now
            }
        return DailyGoal(
            id = DailyGoalId(row[DailyGoalTable.dailyGoalId]),
            goalId = command.goalId,
            memberId = command.memberId,
            date = command.date,
            createdAt = now.toInstant(),
        )
    }

    override fun delete(command: DailyGoalCommand.Remove) {
        DailyGoalTable.deleteWhere {
            (goalId eq command.goalId.value) and
                (memberId eq command.memberId.value) and
                (date eq command.date)
        }
    }

    override fun existsByGoalIdAndMemberIdAndDate(
        goalId: GoalId,
        memberId: MemberId,
        date: LocalDate,
    ): Boolean =
        DailyGoalTable
            .selectAll()
            .where {
                (DailyGoalTable.goalId eq goalId.value) and
                    (DailyGoalTable.memberId eq memberId.value) and
                    (DailyGoalTable.date eq date)
            }.count() > 0
}
