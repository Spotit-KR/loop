package kr.io.team.loop.task.presentation.datafetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import kr.io.team.loop.codegen.types.Goal
import kr.io.team.loop.task.application.dto.GoalTaskStatsDto
import java.util.concurrent.CompletableFuture

@DgsComponent
class GoalTaskStatsDataFetcher {
    @DgsData(parentType = "Goal", field = "totalTaskCount")
    fun totalTaskCount(dfe: DgsDataFetchingEnvironment): CompletableFuture<Int> {
        val goal = dfe.getSource<Goal>()!!
        val dataLoader = dfe.getDataLoader<Long, GoalTaskStatsDto>("goalTaskStats")!!
        return dataLoader.load(goal.id.toLong()).thenApply { it?.totalCount ?: 0 }
    }

    @DgsData(parentType = "Goal", field = "completedTaskCount")
    fun completedTaskCount(dfe: DgsDataFetchingEnvironment): CompletableFuture<Int> {
        val goal = dfe.getSource<Goal>()!!
        val dataLoader = dfe.getDataLoader<Long, GoalTaskStatsDto>("goalTaskStats")!!
        return dataLoader.load(goal.id.toLong()).thenApply { it?.completedCount ?: 0 }
    }

    @DgsData(parentType = "Goal", field = "achievementRate")
    fun achievementRate(dfe: DgsDataFetchingEnvironment): CompletableFuture<Double> {
        val goal = dfe.getSource<Goal>()!!
        val dataLoader = dfe.getDataLoader<Long, GoalTaskStatsDto>("goalTaskStats")!!
        return dataLoader.load(goal.id.toLong()).thenApply { it?.achievementRate ?: 0.0 }
    }
}
