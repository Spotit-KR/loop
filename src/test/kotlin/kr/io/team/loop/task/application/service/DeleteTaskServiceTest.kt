package kr.io.team.loop.task.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

class DeleteTaskServiceTest :
    BehaviorSpec({
        val taskRepository = mockk<TaskRepository>()
        val service = DeleteTaskService(taskRepository)

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

        given("본인 태스크 삭제") {
            val command =
                TaskCommand.Delete(
                    taskId = TaskId(1L),
                    memberId = MemberId(1L),
                )

            `when`("execute 호출") {
                every { taskRepository.findAll(TaskQuery(taskId = TaskId(1L))) } returns listOf(existingTask)
                every { taskRepository.delete(command) } returns existingTask

                then("삭제 성공") {
                    service.execute(command)
                    verify(exactly = 1) { taskRepository.delete(command) }
                }
            }
        }

        given("다른 회원의 태스크 삭제 시도") {
            val command =
                TaskCommand.Delete(
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

        given("존재하지 않는 태스크 삭제 시도") {
            val command =
                TaskCommand.Delete(
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
