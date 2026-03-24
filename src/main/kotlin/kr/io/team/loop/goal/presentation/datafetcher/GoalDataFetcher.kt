package kr.io.team.loop.goal.presentation.datafetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kotlinx.datetime.LocalDate
import kr.io.team.loop.codegen.types.AddDailyGoalInput
import kr.io.team.loop.codegen.types.CreateGoalInput
import kr.io.team.loop.codegen.types.GoalFilter
import kr.io.team.loop.codegen.types.RemoveDailyGoalInput
import kr.io.team.loop.codegen.types.UpdateGoalInput
import kr.io.team.loop.common.config.Authorize
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.goal.application.service.GoalService
import kr.io.team.loop.goal.domain.model.DailyGoalCommand
import kr.io.team.loop.goal.domain.model.Goal
import kr.io.team.loop.goal.domain.model.GoalCommand
import kr.io.team.loop.goal.domain.model.GoalQuery
import kr.io.team.loop.goal.domain.model.GoalTitle
import kr.io.team.loop.codegen.types.Goal as GoalGraphql

@DgsComponent
class GoalDataFetcher(
    private val goalService: GoalService,
) {
    @DgsQuery
    fun myGoals(
        @InputArgument filter: GoalFilter?,
        @Authorize memberId: Long,
    ): List<GoalGraphql> {
        val query =
            GoalQuery(
                memberId = MemberId(memberId),
                id = filter?.id?.let { GoalId(it.toLong()) },
                ids = filter?.ids?.map { GoalId(it.toLong()) },
                title = filter?.title,
                assignedDate = filter?.assignedDate?.let { LocalDate.parse(it) },
            )
        return goalService.findAll(query).map { it.toGraphql() }
    }

    @DgsMutation
    fun createGoal(
        @InputArgument input: CreateGoalInput,
        @Authorize memberId: Long,
    ): GoalGraphql {
        val command =
            GoalCommand.Create(
                title = GoalTitle(input.title),
                memberId = MemberId(memberId),
            )
        return goalService.create(command).toGraphql()
    }

    @DgsMutation
    fun updateGoal(
        @InputArgument input: UpdateGoalInput,
        @Authorize memberId: Long,
    ): GoalGraphql {
        val command =
            GoalCommand.Update(
                goalId = GoalId(input.id.toLong()),
                title = GoalTitle(input.title),
            )
        return goalService.update(command, MemberId(memberId)).toGraphql()
    }

    @DgsMutation
    fun deleteGoal(
        @InputArgument id: String,
        @Authorize memberId: Long,
    ): Boolean {
        val command = GoalCommand.Delete(goalId = GoalId(id.toLong()))
        goalService.delete(command, MemberId(memberId))
        return true
    }

    @DgsMutation
    fun addDailyGoal(
        @InputArgument input: AddDailyGoalInput,
        @Authorize memberId: Long,
    ): GoalGraphql {
        val command =
            DailyGoalCommand.Add(
                goalId = GoalId(input.goalId.toLong()),
                memberId = MemberId(memberId),
                date = LocalDate.parse(input.date),
            )
        return goalService.addDailyGoal(command).toGraphql()
    }

    @DgsMutation
    fun removeDailyGoal(
        @InputArgument input: RemoveDailyGoalInput,
        @Authorize memberId: Long,
    ): Boolean {
        val command =
            DailyGoalCommand.Remove(
                goalId = GoalId(input.goalId.toLong()),
                memberId = MemberId(memberId),
                date = LocalDate.parse(input.date),
            )
        goalService.removeDailyGoal(command)
        return true
    }

    private fun Goal.toGraphql(): GoalGraphql =
        GoalGraphql(
            id = id.value.toString(),
            title = title.value,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt?.toString(),
            // Constructor-required placeholders — always overridden by
            // @DgsData resolvers in GoalTaskStatsDataFetcher (task BC)
            totalTaskCount = 0,
            completedTaskCount = 0,
            achievementRate = 0.0,
        )
}
