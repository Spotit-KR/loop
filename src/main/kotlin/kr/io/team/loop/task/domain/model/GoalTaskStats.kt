package kr.io.team.loop.task.domain.model

import kr.io.team.loop.common.domain.GoalId

data class GoalTaskStats(
    val goalId: GoalId,
    val totalCount: Int,
    val completedCount: Int,
) {
    val achievementRate: Double
        get() = if (totalCount == 0) 0.0 else (completedCount.toDouble() / totalCount) * 100.0
}
