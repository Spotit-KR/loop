package kr.io.team.loop.task.domain.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.TaskId
import java.time.Instant

class TaskTest :
    BehaviorSpec({

        val memberId = MemberId(1L)
        val otherMemberId = MemberId(2L)

        val task =
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

        Given("Task 소유권 검증 시") {
            When("본인 할일이면") {
                Then("true를 반환한다") {
                    task.isOwnedBy(memberId) shouldBe true
                }
            }

            When("본인 할일이 아니면") {
                Then("false를 반환한다") {
                    task.isOwnedBy(otherMemberId) shouldBe false
                }
            }
        }
    })
