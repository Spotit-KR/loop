package kr.io.team.loop.task.domain.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.TaskId
import java.time.LocalDate
import java.time.LocalDateTime

class TaskTest :
    BehaviorSpec({
        given("유효한 데이터") {
            `when`("Task 생성") {
                then("모든 필드 정상") {
                    val now = LocalDateTime.now()
                    val task =
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
                    task.id.value shouldBe 1L
                    task.memberId.value shouldBe 1L
                    task.categoryId.value shouldBe 1L
                    task.title.value shouldBe "회의 준비"
                    task.completed shouldBe false
                    task.taskDate.value shouldBe LocalDate.of(2026, 2, 21)
                }
            }
        }

        given("완료된 태스크") {
            `when`("Task 생성") {
                then("completed = true") {
                    val now = LocalDateTime.now()
                    val task =
                        Task(
                            id = TaskId(1L),
                            memberId = MemberId(1L),
                            categoryId = CategoryId(1L),
                            title = TaskTitle("완료된 태스크"),
                            completed = true,
                            taskDate = TaskDate(LocalDate.of(2026, 2, 21)),
                            createdAt = now,
                            updatedAt = now,
                        )
                    task.completed shouldBe true
                }
            }
        }
    })
