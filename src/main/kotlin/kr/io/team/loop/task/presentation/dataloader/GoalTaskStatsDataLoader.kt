package kr.io.team.loop.task.presentation.dataloader

import com.netflix.graphql.dgs.DgsDataLoader
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.task.application.service.TaskService
import kr.io.team.loop.task.domain.model.GoalTaskStats
import org.dataloader.MappedBatchLoader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

@DgsDataLoader(name = "goalTaskStats")
class GoalTaskStatsDataLoader(
    private val taskService: TaskService,
) : MappedBatchLoader<Long, GoalTaskStats> {
    override fun load(keys: Set<Long>): CompletionStage<Map<Long, GoalTaskStats>> =
        CompletableFuture.supplyAsync {
            val goalIds = keys.map { GoalId(it) }.toSet()
            val stats = taskService.getStatsByGoalIds(goalIds)
            stats.map { (goalId, goalTaskStats) -> goalId.value to goalTaskStats }.toMap()
        }
}
