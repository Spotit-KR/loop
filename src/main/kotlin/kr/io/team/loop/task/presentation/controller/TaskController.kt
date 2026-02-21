package kr.io.team.loop.task.presentation.controller

import kr.io.team.loop.common.config.CurrentMemberId
import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.TaskId
import kr.io.team.loop.task.application.service.CreateTaskService
import kr.io.team.loop.task.application.service.DeleteTaskService
import kr.io.team.loop.task.application.service.GetTaskStatsService
import kr.io.team.loop.task.application.service.GetTasksByDateService
import kr.io.team.loop.task.application.service.ToggleTaskCompleteService
import kr.io.team.loop.task.application.service.UpdateTaskService
import kr.io.team.loop.task.domain.model.TaskCommand
import kr.io.team.loop.task.domain.model.TaskDate
import kr.io.team.loop.task.domain.model.TaskQuery
import kr.io.team.loop.task.domain.model.TaskTitle
import kr.io.team.loop.task.presentation.request.CreateTaskRequest
import kr.io.team.loop.task.presentation.request.UpdateTaskRequest
import kr.io.team.loop.task.presentation.response.TaskResponse
import kr.io.team.loop.task.presentation.response.TaskStatsResponse
import kr.io.team.loop.task.presentation.response.TasksByDateResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/tasks")
class TaskController(
    private val createTaskService: CreateTaskService,
    private val getTasksByDateService: GetTasksByDateService,
    private val updateTaskService: UpdateTaskService,
    private val toggleTaskCompleteService: ToggleTaskCompleteService,
    private val deleteTaskService: DeleteTaskService,
    private val getTaskStatsService: GetTaskStatsService,
) {
    @PostMapping
    fun createTask(
        @RequestBody request: CreateTaskRequest,
        @CurrentMemberId memberId: Long,
    ): ResponseEntity<TaskResponse> {
        val command =
            TaskCommand.Create(
                memberId = MemberId(memberId),
                categoryId = CategoryId(request.categoryId),
                title = TaskTitle(request.title),
                taskDate = TaskDate(LocalDate.parse(request.taskDate)),
            )
        val dto = createTaskService.execute(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(dto))
    }

    @GetMapping
    fun getTasksByDate(
        @RequestParam date: String,
        @CurrentMemberId memberId: Long,
    ): ResponseEntity<TasksByDateResponse> {
        val query =
            TaskQuery(
                memberId = MemberId(memberId),
                taskDate = TaskDate(LocalDate.parse(date)),
            )
        val dto = getTasksByDateService.execute(query)
        return ResponseEntity.ok(TasksByDateResponse.from(dto))
    }

    @PutMapping("/{id}")
    fun updateTask(
        @PathVariable id: Long,
        @RequestBody request: UpdateTaskRequest,
        @CurrentMemberId memberId: Long,
    ): ResponseEntity<TaskResponse> {
        val command =
            TaskCommand.Update(
                taskId = TaskId(id),
                memberId = MemberId(memberId),
                title = TaskTitle(request.title),
                taskDate = TaskDate(LocalDate.parse(request.taskDate)),
            )
        val dto = updateTaskService.execute(command)
        return ResponseEntity.ok(TaskResponse.from(dto))
    }

    @PatchMapping("/{id}/toggle")
    fun toggleComplete(
        @PathVariable id: Long,
        @CurrentMemberId memberId: Long,
    ): ResponseEntity<TaskResponse> {
        val command =
            TaskCommand.ToggleComplete(
                taskId = TaskId(id),
                memberId = MemberId(memberId),
            )
        val dto = toggleTaskCompleteService.execute(command)
        return ResponseEntity.ok(TaskResponse.from(dto))
    }

    @DeleteMapping("/{id}")
    fun deleteTask(
        @PathVariable id: Long,
        @CurrentMemberId memberId: Long,
    ): ResponseEntity<Unit> {
        val command =
            TaskCommand.Delete(
                taskId = TaskId(id),
                memberId = MemberId(memberId),
            )
        deleteTaskService.execute(command)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/stats")
    fun getTaskStats(
        @RequestParam date: String,
        @CurrentMemberId memberId: Long,
    ): ResponseEntity<TaskStatsResponse> {
        val query =
            TaskQuery(
                memberId = MemberId(memberId),
                taskDate = TaskDate(LocalDate.parse(date)),
            )
        val dto = getTaskStatsService.execute(query)
        return ResponseEntity.ok(TaskStatsResponse.from(dto))
    }
}
