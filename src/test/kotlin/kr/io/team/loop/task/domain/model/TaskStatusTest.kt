package kr.io.team.loop.task.domain.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class TaskStatusTest :
    BehaviorSpec({

        Given("TaskStatus") {
            When("TODO 상태이면") {
                Then("이름이 TODO이다") {
                    TaskStatus.TODO.name shouldBe "TODO"
                }
            }

            When("DONE 상태이면") {
                Then("이름이 DONE이다") {
                    TaskStatus.DONE.name shouldBe "DONE"
                }
            }

            When("전체 값 목록은") {
                Then("TODO, DONE 2개이다") {
                    TaskStatus.entries.size shouldBe 2
                }
            }
        }
    })
