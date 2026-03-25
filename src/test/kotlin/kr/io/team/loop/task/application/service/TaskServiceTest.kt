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
import kr.io.team.loop.common.domain.event.DailyGoalRemovedEvent
import kr.io.team.loop.common.domain.event.GoalDeletedEvent
import kr.io.team.loop.common.domain.exception.AccessDeniedException
import kr.io.team.loop.common.domain.exception.EntityNotFoundException
import kr.io.team.loop.task.domain.model.Task
import kr.io.team.loop.task.domain.model.TaskCommand
import kr.io.team.loop.task.domain.model.TaskId
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

        Given("할일 수정 시") {
            When("본인 할일의 상태를 변경하면") {
                val updatedTask = savedTask.copy(status = TaskStatus.DONE, updatedAt = Instant.now())
                val command = TaskCommand.Update(taskId = TaskId(1L), status = TaskStatus.DONE)

                every { taskRepository.findById(TaskId(1L)) } returns savedTask
                every { taskRepository.update(command) } returns updatedTask

                val result = taskService.update(command, memberId)

                Then("변경된 할일을 반환한다") {
                    result.status shouldBe TaskStatus.DONE
                }
            }

            When("본인 할일의 제목을 수정하면") {
                val newTitle = TaskTitle("수학 문제 풀기")
                val updatedTask = savedTask.copy(title = newTitle, updatedAt = Instant.now())
                val command = TaskCommand.Update(taskId = TaskId(1L), title = newTitle)

                every { taskRepository.findById(TaskId(1L)) } returns savedTask
                every { taskRepository.update(command) } returns updatedTask

                val result = taskService.update(command, memberId)

                Then("수정된 할일을 반환한다") {
                    result.title.value shouldBe "수학 문제 풀기"
                }
            }

            When("제목과 상태를 동시에 수정하면") {
                val newTitle = TaskTitle("수학 문제 풀기")
                val updatedTask = savedTask.copy(title = newTitle, status = TaskStatus.DONE, updatedAt = Instant.now())
                val command = TaskCommand.Update(taskId = TaskId(1L), title = newTitle, status = TaskStatus.DONE)

                every { taskRepository.findById(TaskId(1L)) } returns savedTask
                every { taskRepository.update(command) } returns updatedTask

                val result = taskService.update(command, memberId)

                Then("수정된 할일을 반환한다") {
                    result.title.value shouldBe "수학 문제 풀기"
                    result.status shouldBe TaskStatus.DONE
                }
            }

            When("존재하지 않는 할일이면") {
                val command = TaskCommand.Update(taskId = TaskId(99L), status = TaskStatus.DONE)
                every { taskRepository.findById(TaskId(99L)) } returns null

                Then("EntityNotFoundException이 발생한다") {
                    shouldThrow<EntityNotFoundException> {
                        taskService.update(command, memberId)
                    }
                }
            }

            When("본인 할일이 아니면") {
                val command = TaskCommand.Update(taskId = TaskId(1L), status = TaskStatus.DONE)
                every { taskRepository.findById(TaskId(1L)) } returns savedTask

                Then("AccessDeniedException이 발생한다") {
                    shouldThrow<AccessDeniedException> {
                        taskService.update(command, otherMemberId)
                    }
                }
            }
        }

        Given("목표별 할일 통계 조회 시") {
            val goalId1 = GoalId(1L)
            val goalId2 = GoalId(2L)

            fun task(
                goalId: GoalId,
                status: TaskStatus,
            ) = savedTask.copy(goalId = goalId, status = status)

            When("할일이 있는 목표들이면") {
                val goalIds = setOf(goalId1, goalId2)
                val tasks =
                    listOf(
                        task(goalId1, TaskStatus.TODO),
                        task(goalId1, TaskStatus.DONE),
                        task(goalId1, TaskStatus.DONE),
                        task(goalId2, TaskStatus.DONE),
                    )
                every { taskRepository.findAllByGoalIds(goalIds) } returns tasks

                val result = taskService.getStatsByGoalIds(goalIds)

                Then("목표별 통계를 반환한다") {
                    result[goalId1]!!.totalCount shouldBe 3
                    result[goalId1]!!.completedCount shouldBe 2
                    result[goalId2]!!.totalCount shouldBe 1
                    result[goalId2]!!.completedCount shouldBe 1
                    result[goalId2]!!.achievementRate shouldBe 100.0
                }
            }

            When("빈 goalIds이면") {
                val goalIds = emptySet<GoalId>()
                every { taskRepository.findAllByGoalIds(goalIds) } returns emptyList()

                val result = taskService.getStatsByGoalIds(goalIds)

                Then("빈 맵을 반환한다") {
                    result shouldBe emptyMap()
                }
            }
        }

        Given("GoalDeletedEvent 수신 시") {
            When("해당 goalId의 Task가 있으면") {
                val event = GoalDeletedEvent(goalId = GoalId(1L))
                justRun { taskRepository.deleteByGoalId(GoalId(1L)) }

                taskService.handleGoalDeleted(event)

                Then("해당 goalId의 모든 Task가 삭제된다") {
                    verify { taskRepository.deleteByGoalId(GoalId(1L)) }
                }
            }
        }

        Given("DailyGoalRemovedEvent 수신 시") {
            When("해당 goalId와 date의 Task가 있으면") {
                val date = LocalDate(2025, 2, 20)
                val event = DailyGoalRemovedEvent(goalId = GoalId(1L), date = date)
                justRun { taskRepository.deleteByGoalIdAndTaskDate(GoalId(1L), date) }

                taskService.handleDailyGoalRemoved(event)

                Then("해당 goalId와 date의 Task가 삭제된다") {
                    verify { taskRepository.deleteByGoalIdAndTaskDate(GoalId(1L), date) }
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
