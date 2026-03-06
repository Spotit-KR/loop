package kr.io.team.loop.task.domain.repository

import kr.io.team.loop.common.domain.TaskId
import kr.io.team.loop.task.domain.model.Task
import kr.io.team.loop.task.domain.model.TaskCommand
import kr.io.team.loop.task.domain.model.TaskQuery

interface TaskRepository {
    fun save(command: TaskCommand.Create): Task

    fun update(command: TaskCommand.Update): Task

    fun delete(command: TaskCommand.Delete)

    fun findAll(query: TaskQuery): List<Task>

    fun findById(id: TaskId): Task?
}
