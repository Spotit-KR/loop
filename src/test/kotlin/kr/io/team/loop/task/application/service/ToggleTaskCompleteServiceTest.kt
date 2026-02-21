package kr.io.team.loop.task.application.service

import io.kotest.assertions.throwables.shouldThrow
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
import kr.io.team.loop.task.domain.model.TaskQuery
import kr.io.team.loop.task.domain.model.TaskTitle
import kr.io.team.loop.task.domain.repository.TaskRepository
import java.time.LocalDate
import java.time.LocalDateTime

class ToggleTaskCompleteServiceTest :
    BehaviorSpec({
        val taskRepository = mockk<TaskRepository>()
        val service = ToggleTaskCompleteService(taskRepository)

        val now = LocalDateTime.now()
        val existingTask =
            Task(
                id = TaskId(1L),
                memberId = MemberId(1L),
                categoryId = CategoryId(1L),
                title = TaskTitle("회의 준비"),
                completed = false,
                taskDate = TaskDate(LocalDate.of(2026, 2, 21)),
                createdAt = now,
                updatedAt = now,
            )

        given("미완료 태스크를 본인이 토글") {
            val command =
                TaskCommand.ToggleComplete(
                    taskId = TaskId(1L),
                    memberId = MemberId(1L),
                )
            val toggledTask = existingTask.copy(completed = true)

            `when`("execute 호출") {
                every { taskRepository.findAll(TaskQuery(taskId = TaskId(1L))) } returns listOf(existingTask)
                every { taskRepository.save(command) } returns toggledTask

                then("완료 상태로 전환된 TaskDto 반환") {
                    val result = service.execute(command)
                    result.completed shouldBe true
                }
            }
        }

        given("다른 회원의 태스크 토글 시도") {
            val command =
                TaskCommand.ToggleComplete(
                    taskId = TaskId(1L),
                    memberId = MemberId(99L),
                )

            `when`("execute 호출") {
                every { taskRepository.findAll(TaskQuery(taskId = TaskId(1L))) } returns listOf(existingTask)

                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        service.execute(command)
                    }
                }
            }
        }

        given("존재하지 않는 태스크 토글 시도") {
            val command =
                TaskCommand.ToggleComplete(
                    taskId = TaskId(999L),
                    memberId = MemberId(1L),
                )

            `when`("execute 호출") {
                every { taskRepository.findAll(TaskQuery(taskId = TaskId(999L))) } returns emptyList()

                then("NoSuchElementException 발생") {
                    shouldThrow<NoSuchElementException> {
                        service.execute(command)
                    }
                }
            }
        }
    })
