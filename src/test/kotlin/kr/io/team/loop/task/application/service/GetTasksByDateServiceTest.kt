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

class GetTasksByDateServiceTest :
    BehaviorSpec({
        val taskReadRepository = mockk<TaskReadRepository>()
        val service = GetTasksByDateService(taskReadRepository)

        given("날짜별 태스크가 여러 카테고리에 존재") {
            val query =
                TaskQuery(
                    memberId = MemberId(1L),
                    taskDate = TaskDate(LocalDate.of(2026, 2, 21)),
                )
            val now = LocalDateTime.now()
            val tasksWithCategoryInfo =
                listOf(
                    TaskWithCategoryInfo(
                        id = TaskId(1L),
                        memberId = MemberId(1L),
                        categoryId = CategoryId(1L),
                        title = TaskTitle("업무 태스크"),
                        completed = false,
                        taskDate = TaskDate(LocalDate.of(2026, 2, 21)),
                        categoryName = "업무",
                        categoryColor = "#FF5733",
                        createdAt = now,
                        updatedAt = now,
                    ),
                    TaskWithCategoryInfo(
                        id = TaskId(2L),
                        memberId = MemberId(1L),
                        categoryId = CategoryId(2L),
                        title = TaskTitle("개인 태스크"),
                        completed = true,
                        taskDate = TaskDate(LocalDate.of(2026, 2, 21)),
                        categoryName = "개인",
                        categoryColor = "#3366FF",
                        createdAt = now,
                        updatedAt = now,
                    ),
                )

            `when`("execute 호출") {
                every { taskReadRepository.findAllWithCategoryInfo(query) } returns tasksWithCategoryInfo

                then("날짜별 카테고리 그룹핑된 DTO 반환") {
                    val result = service.execute(query)
                    result.date shouldBe LocalDate.of(2026, 2, 21)
                    result.categories.size shouldBe 2
                    result.categories[0].categoryName shouldBe "업무"
                    result.categories[0].tasks.size shouldBe 1
                    result.categories[1].categoryName shouldBe "개인"
                    result.categories[1].tasks.size shouldBe 1
                }
            }
        }

        given("같은 카테고리에 여러 태스크") {
            val query =
                TaskQuery(
                    memberId = MemberId(1L),
                    taskDate = TaskDate(LocalDate.of(2026, 2, 21)),
                )
            val now = LocalDateTime.now()
            val tasksWithCategoryInfo =
                listOf(
                    TaskWithCategoryInfo(
                        id = TaskId(1L),
                        memberId = MemberId(1L),
                        categoryId = CategoryId(1L),
                        title = TaskTitle("업무 태스크 1"),
                        completed = false,
                        taskDate = TaskDate(LocalDate.of(2026, 2, 21)),
                        categoryName = "업무",
                        categoryColor = "#FF5733",
                        createdAt = now,
                        updatedAt = now,
                    ),
                    TaskWithCategoryInfo(
                        id = TaskId(2L),
                        memberId = MemberId(1L),
                        categoryId = CategoryId(1L),
                        title = TaskTitle("업무 태스크 2"),
                        completed = true,
                        taskDate = TaskDate(LocalDate.of(2026, 2, 21)),
                        categoryName = "업무",
                        categoryColor = "#FF5733",
                        createdAt = now,
                        updatedAt = now,
                    ),
                )

            `when`("execute 호출") {
                every { taskReadRepository.findAllWithCategoryInfo(query) } returns tasksWithCategoryInfo

                then("하나의 카테고리 그룹으로 묶임") {
                    val result = service.execute(query)
                    result.categories.size shouldBe 1
                    result.categories[0].tasks.size shouldBe 2
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

                then("빈 카테고리 리스트 반환") {
                    val result = service.execute(query)
                    result.date shouldBe LocalDate.of(2026, 2, 22)
                    result.categories shouldBe emptyList()
                }
            }
        }
    })
