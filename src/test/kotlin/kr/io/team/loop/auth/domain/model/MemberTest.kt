package kr.io.team.loop.auth.domain.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kr.io.team.loop.common.domain.MemberId
import java.time.Instant

class MemberTest :
    BehaviorSpec({

        Given("Member 생성 시") {
            When("필수 필드가 모두 있으면") {
                val member =
                    Member(
                        id = MemberId(1L),
                        loginId = LoginId("testuser"),
                        nickname = Nickname("홍길동"),
                        password = "hashed_password",
                        createdAt = Instant.now(),
                        updatedAt = null,
                    )

                Then("정상 생성된다") {
                    member.id.value shouldBe 1L
                    member.loginId.value shouldBe "testuser"
                    member.nickname.value shouldBe "홍길동"
                    member.password shouldBe "hashed_password"
                    member.createdAt shouldNotBe null
                    member.updatedAt shouldBe null
                }
            }
        }

        Given("MemberCommand.Register 생성 시") {
            When("유효한 값이면") {
                val command =
                    MemberCommand.Register(
                        loginId = LoginId("newuser"),
                        nickname = Nickname("새사용자"),
                        rawPassword = "password123",
                    )

                Then("정상 생성된다") {
                    command.loginId.value shouldBe "newuser"
                    command.nickname.value shouldBe "새사용자"
                    command.rawPassword shouldBe "password123"
                }
            }

            When("encodedPassword를 포함하면") {
                val command =
                    MemberCommand.Register(
                        loginId = LoginId("newuser"),
                        nickname = Nickname("새사용자"),
                        rawPassword = "password123",
                        encodedPassword = "encoded_password",
                    )

                Then("encodedPassword가 설정된다") {
                    command.encodedPassword shouldBe "encoded_password"
                }
            }

            When("encodedPassword를 생략하면") {
                val command =
                    MemberCommand.Register(
                        loginId = LoginId("newuser"),
                        nickname = Nickname("새사용자"),
                        rawPassword = "password123",
                    )

                Then("encodedPassword는 null이다") {
                    command.encodedPassword shouldBe null
                }
            }
        }

        Given("MemberCommand.Login 생성 시") {
            When("유효한 값이면") {
                val command =
                    MemberCommand.Login(
                        loginId = LoginId("testuser"),
                        rawPassword = "password123",
                    )

                Then("정상 생성된다") {
                    command.loginId.value shouldBe "testuser"
                    command.rawPassword shouldBe "password123"
                }
            }
        }
    })
