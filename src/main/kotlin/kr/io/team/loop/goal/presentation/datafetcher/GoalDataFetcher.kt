package kr.io.team.loop.goal.presentation.datafetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.io.team.loop.codegen.types.CreateGoalInput
import kr.io.team.loop.codegen.types.UpdateGoalInput
import kr.io.team.loop.common.config.Authorize
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.goal.application.service.GoalService
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
        @Authorize memberId: Long,
    ): List<GoalGraphql> {
        val query = GoalQuery(memberId = MemberId(memberId))
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

    private fun Goal.toGraphql(): GoalGraphql =
        GoalGraphql(
            id = id.value.toString(),
            title = title.value,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt?.toString(),
        )
}
