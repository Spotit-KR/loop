package kr.io.team.loop.task.application.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.TaskId
import kr.io.team.loop.task.domain.model.TaskDate
import kr.io.team.loop.task.domain.model.TaskQuery
import kr.io.team.loop.task.domain.model.TaskTitle
import kr.io.team.loop.task.domain.model.TaskWithCategoryInfo
import kr.io.team.loop.task.domain.repository.TaskReadRepository
import java.time.LocalDate
import java.time.LocalDateTime

class GetTaskStatsServiceTest :
    BehaviorSpec({
        val taskReadRepository = mockk<TaskReadRepository>()
        val service = GetTaskStatsService(taskReadRepository)

        val now = LocalDateTime.now()

        fun makeTask(
            id: Long,
            completed: Boolean,
            date: LocalDate,
        ) = TaskWithCategoryInfo(
            id = TaskId(id),
            memberId = MemberId(1L),
            categoryId = CategoryId(1L),
            title = TaskTitle("Task $id"),
            completed = completed,
            taskDate = TaskDate(date),
            categoryName = "업무",
            categoryColor = "#FF5733",
            createdAt = now,
            updatedAt = now,
        )

        given("태스크 10개 중 4개 완료") {
            val query =
                TaskQuery(
                    memberId = MemberId(1L),
                    taskDate = TaskDate(LocalDate.of(2026, 2, 21)),
                )
            val tasks = (1..10).map { makeTask(it.toLong(), it <= 4, LocalDate.of(2026, 2, 21)) }

            `when`("execute 호출") {
                every { taskReadRepository.findAllWithCategoryInfo(query) } returns tasks

                then("total=10, completed=4, rate=40.0 반환") {
                    val result = service.execute(query)
                    result.total shouldBe 10
                    result.completed shouldBe 4
                    result.rate shouldBe 40.0
                }
            }
        }

        given("태스크가 없는 날짜") {
            val query =
                TaskQuery(
                    memberId = MemberId(1L),
                    taskDate = TaskDate(LocalDate.of(2026, 2, 22)),
                )

            `when`("execute 호출") {
                every { taskReadRepository.findAllWithCategoryInfo(query) } returns emptyList()

                then("total=0, completed=0, rate=0.0 반환") {
                    val result = service.execute(query)
                    result.total shouldBe 0
                    result.completed shouldBe 0
                    result.rate shouldBe 0.0
                }
            }
        }

        given("태스크 3개 모두 완료") {
            val query =
                TaskQuery(
                    memberId = MemberId(1L),
                    taskDate = TaskDate(LocalDate.of(2026, 2, 21)),
                )
            val tasks = (1..3).map { makeTask(it.toLong(), true, LocalDate.of(2026, 2, 21)) }

            `when`("execute 호출") {
                every { taskReadRepository.findAllWithCategoryInfo(query) } returns tasks

                then("rate=100.0 반환") {
                    val result = service.execute(query)
                    result.rate shouldBe 100.0
                }
            }
        }
    })
