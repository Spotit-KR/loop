package kr.io.team.loop.task.domain.repository

import kr.io.team.loop.task.domain.model.TaskQuery
import kr.io.team.loop.task.domain.model.TaskWithCategoryInfo

interface TaskReadRepository {
    fun findAllWithCategoryInfo(query: TaskQuery): List<TaskWithCategoryInfo>
}
