package kr.io.team.loop.member.domain.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kr.io.team.loop.common.domain.Email
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.Nickname
import java.time.LocalDateTime

class MemberTest :
    BehaviorSpec({
        given("유효한 데이터") {
            `when`("Member 생성") {
                then("모든 필드 정상") {
                    val now = LocalDateTime.now()
                    val member =
                        Member(
                            id = MemberId(1L),
                            email = Email("test@example.com"),
                            hashedPassword = HashedPassword("hashed_password"),
                            nickname = Nickname("testuser"),
                            profileImageUrl = ProfileImageUrl("https://example.com/image.png"),
                            createdAt = now,
                            updatedAt = now,
                        )
                    member.id.value shouldBe 1L
                    member.email.value shouldBe "test@example.com"
                    member.nickname.value shouldBe "testuser"
                    member.profileImageUrl?.value shouldBe "https://example.com/image.png"
                }
            }
        }

        given("profileImageUrl null") {
            `when`("Member 생성") {
                then("profileImageUrl null") {
                    val now = LocalDateTime.now()
                    val member =
                        Member(
                            id = MemberId(1L),
                            email = Email("test@example.com"),
                            hashedPassword = HashedPassword("hashed_password"),
                            nickname = Nickname("testuser"),
                            profileImageUrl = null,
                            createdAt = now,
                            updatedAt = now,
                        )
                    member.profileImageUrl.shouldBeNull()
                }
            }
        }
    })
