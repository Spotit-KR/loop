package kr.io.team.loop.task.domain.repository

import kr.io.team.loop.task.domain.model.Task
import kr.io.team.loop.task.domain.model.TaskCommand
import kr.io.team.loop.task.domain.model.TaskQuery

interface TaskRepository {
    fun save(command: TaskCommand): Task

    fun findAll(query: TaskQuery): List<Task>

    fun delete(command: TaskCommand.Delete): Task
}
