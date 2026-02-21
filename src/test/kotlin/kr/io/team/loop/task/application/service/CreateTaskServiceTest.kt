package kr.io.team.loop.task.application.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.TaskId
import kr.io.team.loop.task.domain.model.Task
import kr.io.team.loop.task.domain.model.TaskCommand
import kr.io.team.loop.task.domain.model.TaskDate
import kr.io.team.loop.task.domain.model.TaskTitle
import kr.io.team.loop.task.domain.repository.TaskRepository
import java.time.LocalDate
import java.time.LocalDateTime

class CreateTaskServiceTest :
    BehaviorSpec({
        val taskRepository = mockk<TaskRepository>()
        val service = CreateTaskService(taskRepository)

        given("유효한 Create 커맨드") {
            val command =
                TaskCommand.Create(
                    memberId = MemberId(1L),
                    categoryId = CategoryId(1L),
                    title = TaskTitle("회의 준비"),
                    taskDate = TaskDate(LocalDate.of(2026, 2, 21)),
                )
            val now = LocalDateTime.now()
            val task =
                Task(
                    id = TaskId(1L),
                    memberId = command.memberId,
                    categoryId = command.categoryId,
                    title = command.title,
                    completed = false,
                    taskDate = command.taskDate,
                    createdAt = now,
                    updatedAt = now,
                )

            `when`("execute 호출") {
                every { taskRepository.save(command) } returns task

                then("TaskDto 반환") {
                    val result = service.execute(command)
                    result.id.value shouldBe 1L
                    result.memberId.value shouldBe 1L
                    result.categoryId.value shouldBe 1L
                    result.title.value shouldBe "회의 준비"
                    result.completed shouldBe false
                    result.taskDate.value shouldBe LocalDate.of(2026, 2, 21)
                }
            }
        }
    })
