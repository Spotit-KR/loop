package kr.io.team.loop.task.presentation.response

import kr.io.team.loop.task.application.dto.TaskStatsDto

data class TaskStatsResponse(
    val total: Int,
    val completed: Int,
    val rate: Double,
) {
    companion object {
        fun from(dto: TaskStatsDto) =
            TaskStatsResponse(
                total = dto.total,
                completed = dto.completed,
                rate = dto.rate,
            )
    }
}
