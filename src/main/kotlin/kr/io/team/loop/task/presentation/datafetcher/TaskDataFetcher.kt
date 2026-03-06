package kr.io.team.loop.task.presentation.datafetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kotlinx.datetime.LocalDate
import kr.io.team.loop.codegen.types.CreateTaskInput
import kr.io.team.loop.codegen.types.TaskFilter
import kr.io.team.loop.codegen.types.UpdateTaskInput
import kr.io.team.loop.common.config.Authorize
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.TaskId
import kr.io.team.loop.task.application.service.TaskService
import kr.io.team.loop.task.domain.model.Task
import kr.io.team.loop.task.domain.model.TaskCommand
import kr.io.team.loop.task.domain.model.TaskQuery
import kr.io.team.loop.task.domain.model.TaskStatus
import kr.io.team.loop.task.domain.model.TaskTitle
import kr.io.team.loop.codegen.types.Task as TaskGraphql
import kr.io.team.loop.codegen.types.TaskStatus as TaskStatusGraphql

@DgsComponent
class TaskDataFetcher(
    private val taskService: TaskService,
) {
    @DgsQuery
    fun myTasks(
        @InputArgument filter: TaskFilter,
        @Authorize memberId: Long,
    ): List<TaskGraphql> {
        val query =
            TaskQuery(
                memberId = MemberId(memberId),
                goalId = filter.goalId?.let { GoalId(it.toLong()) },
                startDate = filter.startDate?.let { LocalDate.parse(it) },
                endDate = filter.endDate?.let { LocalDate.parse(it) },
            )
        return taskService.findAll(query).map { it.toGraphql() }
    }

    @DgsMutation
    fun createTask(
        @InputArgument input: CreateTaskInput,
        @Authorize memberId: Long,
    ): TaskGraphql {
        val command =
            TaskCommand.Create(
                title = TaskTitle(input.title),
                goalId = GoalId(input.goalId.toLong()),
                memberId = MemberId(memberId),
                taskDate = LocalDate.parse(input.date),
            )
        return taskService.create(command).toGraphql()
    }

    @DgsMutation
    fun updateTask(
        @InputArgument input: UpdateTaskInput,
        @Authorize memberId: Long,
    ): TaskGraphql {
        val command =
            TaskCommand.Update(
                taskId = TaskId(input.id.toLong()),
                title = input.title?.let { TaskTitle(it) },
                status = input.status?.let { TaskStatus.valueOf(it.name) },
            )
        return taskService.update(command, MemberId(memberId)).toGraphql()
    }

    @DgsMutation
    fun deleteTask(
        @InputArgument id: String,
        @Authorize memberId: Long,
    ): Boolean {
        val command = TaskCommand.Delete(taskId = TaskId(id.toLong()))
        taskService.delete(command, MemberId(memberId))
        return true
    }

    private fun Task.toGraphql(): TaskGraphql =
        TaskGraphql(
            id = id.value.toString(),
            title = title.value,
            status = TaskStatusGraphql.valueOf(status.name),
            goalId = goalId.value.toString(),
            taskDate = taskDate.toString(),
            createdAt = createdAt.toString(),
            updatedAt = updatedAt?.toString(),
        )
}
