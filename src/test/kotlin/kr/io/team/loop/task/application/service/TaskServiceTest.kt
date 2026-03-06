package kr.io.team.loop.task.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.TaskId
import kr.io.team.loop.common.domain.exception.AccessDeniedException
import kr.io.team.loop.common.domain.exception.EntityNotFoundException
import kr.io.team.loop.task.domain.model.Task
import kr.io.team.loop.task.domain.model.TaskCommand
import kr.io.team.loop.task.domain.model.TaskQuery
import kr.io.team.loop.task.domain.model.TaskStatus
import kr.io.team.loop.task.domain.model.TaskTitle
import kr.io.team.loop.task.domain.repository.TaskRepository
import java.time.Instant

class TaskServiceTest :
    BehaviorSpec({

        val taskRepository = mockk<TaskRepository>()
        val taskService = TaskService(taskRepository)

        val memberId = MemberId(1L)
        val otherMemberId = MemberId(2L)

        val savedTask =
            Task(
                id = TaskId(1L),
                title = TaskTitle("영어 단어 외우기"),
                status = TaskStatus.TODO,
                goalId = GoalId(1L),
                memberId = memberId,
                taskDate = LocalDate(2025, 2, 20),
                createdAt = Instant.now(),
                updatedAt = null,
            )

        Given("할일 생성 시") {
            When("유효한 입력이면") {
                val command =
                    TaskCommand.Create(
                        title = TaskTitle("영어 단어 외우기"),
                        goalId = GoalId(1L),
                        memberId = memberId,
                        taskDate = LocalDate(2025, 2, 20),
                    )
                every { taskRepository.save(command) } returns savedTask

                val result = taskService.create(command)

                Then("생성된 할일을 반환한다") {
                    result.id.value shouldBe 1L
                    result.title.value shouldBe "영어 단어 외우기"
                    result.status shouldBe TaskStatus.TODO
                    result.goalId.value shouldBe 1L
                    result.memberId shouldBe memberId
                }
            }
        }

        Given("할일 목록 조회 시") {
            When("해당 사용자의 할일이 있으면") {
                val query = TaskQuery(memberId = memberId)
                every { taskRepository.findAll(query) } returns listOf(savedTask)

                val result = taskService.findAll(query)

                Then("할일 목록을 반환한다") {
                    result shouldHaveSize 1
                    result[0].title.value shouldBe "영어 단어 외우기"
                }
            }

            When("할일이 없으면") {
                val query = TaskQuery(memberId = otherMemberId)
                every { taskRepository.findAll(query) } returns emptyList()

                val result = taskService.findAll(query)

                Then("빈 목록을 반환한다") {
                    result shouldHaveSize 0
                }
            }
        }

        Given("할일 상태 변경 시") {
            When("본인 할일이면") {
                val updatedTask = savedTask.copy(status = TaskStatus.DONE, updatedAt = Instant.now())
                val command = TaskCommand.UpdateStatus(taskId = TaskId(1L), status = TaskStatus.DONE)

                every { taskRepository.findById(TaskId(1L)) } returns savedTask
                every { taskRepository.updateStatus(command) } returns updatedTask

                val result = taskService.updateStatus(command, memberId)

                Then("변경된 할일을 반환한다") {
                    result.status shouldBe TaskStatus.DONE
                }
            }

            When("존재하지 않는 할일이면") {
                val command = TaskCommand.UpdateStatus(taskId = TaskId(99L), status = TaskStatus.DONE)
                every { taskRepository.findById(TaskId(99L)) } returns null

                Then("EntityNotFoundException이 발생한다") {
                    shouldThrow<EntityNotFoundException> {
                        taskService.updateStatus(command, memberId)
                    }
                }
            }

            When("본인 할일이 아니면") {
                val command = TaskCommand.UpdateStatus(taskId = TaskId(1L), status = TaskStatus.DONE)
                every { taskRepository.findById(TaskId(1L)) } returns savedTask

                Then("AccessDeniedException이 발생한다") {
                    shouldThrow<AccessDeniedException> {
                        taskService.updateStatus(command, otherMemberId)
                    }
                }
            }
        }

        Given("할일 제목 수정 시") {
            When("본인 할일이면") {
                val newTitle = TaskTitle("수학 문제 풀기")
                val updatedTask = savedTask.copy(title = newTitle, updatedAt = Instant.now())
                val command = TaskCommand.UpdateTitle(taskId = TaskId(1L), title = newTitle)

                every { taskRepository.findById(TaskId(1L)) } returns savedTask
                every { taskRepository.updateTitle(command) } returns updatedTask

                val result = taskService.updateTitle(command, memberId)

                Then("수정된 할일을 반환한다") {
                    result.title.value shouldBe "수학 문제 풀기"
                }
            }

            When("존재하지 않는 할일이면") {
                val command = TaskCommand.UpdateTitle(taskId = TaskId(99L), title = TaskTitle("새 제목"))
                every { taskRepository.findById(TaskId(99L)) } returns null

                Then("EntityNotFoundException이 발생한다") {
                    shouldThrow<EntityNotFoundException> {
                        taskService.updateTitle(command, memberId)
                    }
                }
            }

            When("본인 할일이 아니면") {
                val command = TaskCommand.UpdateTitle(taskId = TaskId(1L), title = TaskTitle("새 제목"))
                every { taskRepository.findById(TaskId(1L)) } returns savedTask

                Then("AccessDeniedException이 발생한다") {
                    shouldThrow<AccessDeniedException> {
                        taskService.updateTitle(command, otherMemberId)
                    }
                }
            }
        }

        Given("할일 삭제 시") {
            When("본인 할일이면") {
                val command = TaskCommand.Delete(taskId = TaskId(1L))
                every { taskRepository.findById(TaskId(1L)) } returns savedTask
                justRun { taskRepository.delete(command) }

                taskService.delete(command, memberId)

                Then("삭제가 수행된다") {
                    verify { taskRepository.delete(command) }
                }
            }

            When("존재하지 않는 할일이면") {
                val command = TaskCommand.Delete(taskId = TaskId(99L))
                every { taskRepository.findById(TaskId(99L)) } returns null

                Then("EntityNotFoundException이 발생한다") {
                    shouldThrow<EntityNotFoundException> {
                        taskService.delete(command, memberId)
                    }
                }
            }

            When("본인 할일이 아니면") {
                val command = TaskCommand.Delete(taskId = TaskId(1L))
                every { taskRepository.findById(TaskId(1L)) } returns savedTask

                Then("AccessDeniedException이 발생한다") {
                    shouldThrow<AccessDeniedException> {
                        taskService.delete(command, otherMemberId)
                    }
                }
            }
        }
    })
