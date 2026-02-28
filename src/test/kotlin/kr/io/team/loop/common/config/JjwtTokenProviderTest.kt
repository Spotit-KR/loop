package kr.io.team.loop.common.config

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class JjwtTokenProviderTest :
    BehaviorSpec({

        val secret = "test-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256"
        val expirationMs = 3600000L
        val provider = JjwtTokenProvider(secret, expirationMs)

        Given("validateToken") {
            When("유효한 토큰이면") {
                val token = provider.generateToken(1L)

                Then("true를 반환한다") {
                    provider.validateToken(token) shouldBe true
                }
            }

            When("변조된 토큰이면") {
                val token = provider.generateToken(1L) + "tampered"

                Then("false를 반환한다") {
                    provider.validateToken(token) shouldBe false
                }
            }

            When("다른 키로 서명된 토큰이면") {
                val otherProvider =
                    JjwtTokenProvider(
                        "other-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256",
                        expirationMs,
                    )
                val token = otherProvider.generateToken(1L)

                Then("false를 반환한다") {
                    provider.validateToken(token) shouldBe false
                }
            }

            When("만료된 토큰이면") {
                val expiredProvider = JjwtTokenProvider(secret, -1000L)
                val token = expiredProvider.generateToken(1L)

                Then("false를 반환한다") {
                    provider.validateToken(token) shouldBe false
                }
            }

            When("빈 문자열이면") {
                Then("false를 반환한다") {
                    provider.validateToken("") shouldBe false
                }
            }
        }

        Given("getMemberIdFromToken") {
            When("유효한 토큰이면") {
                val token = provider.generateToken(42L)

                Then("정확한 memberId를 반환한다") {
                    provider.getMemberIdFromToken(token) shouldBe 42L
                }
            }
        }
    })
