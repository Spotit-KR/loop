package kr.io.team.loop.task.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class TaskTitleTest :
    BehaviorSpec({
        given("공백이 아닌 200자 이하의 문자열") {
            `when`("TaskTitle 생성") {
                then("정상 생성") {
                    val title = TaskTitle("회의 준비")
                    title.value shouldBe "회의 준비"
                }
            }
        }

        given("빈 문자열") {
            `when`("TaskTitle 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        TaskTitle("")
                    }
                }
            }
        }

        given("공백만 있는 문자열") {
            `when`("TaskTitle 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        TaskTitle("   ")
                    }
                }
            }
        }

        given("201자 문자열") {
            `when`("TaskTitle 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        TaskTitle("a".repeat(201))
                    }
                }
            }
        }

        given("정확히 200자 문자열") {
            `when`("TaskTitle 생성") {
                then("정상 생성") {
                    val title = TaskTitle("a".repeat(200))
                    title.value shouldBe "a".repeat(200)
                }
            }
        }
    })
