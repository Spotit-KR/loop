package kr.io.team.loop.goal.domain.repository

import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.goal.domain.model.DailyGoal
import kr.io.team.loop.goal.domain.model.DailyGoalCommand

interface DailyGoalRepository {
    fun save(command: DailyGoalCommand.Add): DailyGoal

    fun delete(command: DailyGoalCommand.Remove)

    fun deleteByGoalId(goalId: GoalId)

    fun existsByGoalIdAndMemberIdAndDate(
        goalId: GoalId,
        memberId: MemberId,
        date: LocalDate,
    ): Boolean
}
