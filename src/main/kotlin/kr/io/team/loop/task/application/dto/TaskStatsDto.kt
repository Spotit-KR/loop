package kr.io.team.loop.task.application.dto

data class TaskStatsDto(
    val total: Int,
    val completed: Int,
    val rate: Double,
)
