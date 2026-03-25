package kr.io.team.loop.goal.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.event.DailyGoalRemovedEvent
import kr.io.team.loop.common.domain.event.GoalDeletedEvent
import kr.io.team.loop.common.domain.exception.AccessDeniedException
import kr.io.team.loop.common.domain.exception.DuplicateEntityException
import kr.io.team.loop.common.domain.exception.EntityNotFoundException
import kr.io.team.loop.goal.domain.model.DailyGoal
import kr.io.team.loop.goal.domain.model.DailyGoalCommand
import kr.io.team.loop.goal.domain.model.DailyGoalId
import kr.io.team.loop.goal.domain.model.Goal
import kr.io.team.loop.goal.domain.model.GoalCommand
import kr.io.team.loop.goal.domain.model.GoalQuery
import kr.io.team.loop.goal.domain.model.GoalTitle
import kr.io.team.loop.goal.domain.repository.DailyGoalRepository
import kr.io.team.loop.goal.domain.repository.GoalRepository
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant

class GoalServiceTest :
    BehaviorSpec({

        val goalRepository = mockk<GoalRepository>()
        val dailyGoalRepository = mockk<DailyGoalRepository>()
        val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
        val goalService = GoalService(goalRepository, dailyGoalRepository, eventPublisher)

        val memberId = MemberId(1L)
        val otherMemberId = MemberId(2L)

        val savedGoal =
            Goal(
                id = GoalId(1L),
                title = GoalTitle("영어 공부"),
                memberId = memberId,
                createdAt = Instant.now(),
                updatedAt = null,
            )

        Given("목표 생성 시") {
            When("유효한 입력이면") {
                val command = GoalCommand.Create(title = GoalTitle("영어 공부"), memberId = memberId)
                every { goalRepository.save(command) } returns savedGoal

                val result = goalService.create(command)

                Then("생성된 목표를 반환한다") {
                    result.id.value shouldBe 1L
                    result.title.value shouldBe "영어 공부"
                    result.memberId shouldBe memberId
                }
            }
        }

        Given("목표 목록 조회 시") {
            When("해당 사용자의 목표가 있으면") {
                val query = GoalQuery(memberId = memberId)
                every { goalRepository.findAll(query) } returns listOf(savedGoal)

                val result = goalService.findAll(query)

                Then("목표 목록을 반환한다") {
                    result shouldHaveSize 1
                    result[0].title.value shouldBe "영어 공부"
                }
            }

            When("목표가 없으면") {
                val query = GoalQuery(memberId = otherMemberId)
                every { goalRepository.findAll(query) } returns emptyList()

                val result = goalService.findAll(query)

                Then("빈 목록을 반환한다") {
                    result shouldHaveSize 0
                }
            }
        }

        Given("목표 수정 시") {
            When("본인 목표이면") {
                val updatedGoal = savedGoal.copy(title = GoalTitle("수학 공부"), updatedAt = Instant.now())
                val command = GoalCommand.Update(goalId = GoalId(1L), title = GoalTitle("수학 공부"))

                every { goalRepository.findById(GoalId(1L)) } returns savedGoal
                every { goalRepository.update(command) } returns updatedGoal

                val result = goalService.update(command, memberId)

                Then("수정된 목표를 반환한다") {
                    result.title.value shouldBe "수학 공부"
                }
            }

            When("존재하지 않는 목표이면") {
                val command = GoalCommand.Update(goalId = GoalId(99L), title = GoalTitle("수학 공부"))
                every { goalRepository.findById(GoalId(99L)) } returns null

                Then("EntityNotFoundException이 발생한다") {
                    shouldThrow<EntityNotFoundException> {
                        goalService.update(command, memberId)
                    }
                }
            }

            When("본인 목표가 아니면") {
                val command = GoalCommand.Update(goalId = GoalId(1L), title = GoalTitle("수학 공부"))
                every { goalRepository.findById(GoalId(1L)) } returns savedGoal

                Then("AccessDeniedException이 발생한다") {
                    shouldThrow<AccessDeniedException> {
                        goalService.update(command, otherMemberId)
                    }
                }
            }
        }

        Given("목표 삭제 시") {
            When("본인 목표이면") {
                val command = GoalCommand.Delete(goalId = GoalId(1L))
                every { goalRepository.findById(GoalId(1L)) } returns savedGoal
                justRun { goalRepository.delete(command) }

                goalService.delete(command, memberId)

                Then("삭제가 수행된다") {
                    verify { goalRepository.delete(command) }
                }

                Then("GoalDeletedEvent가 발행된다") {
                    val eventSlot = slot<GoalDeletedEvent>()
                    verify { eventPublisher.publishEvent(capture(eventSlot)) }
                    eventSlot.captured.goalId shouldBe GoalId(1L)
                }
            }

            When("존재하지 않는 목표이면") {
                val command = GoalCommand.Delete(goalId = GoalId(99L))
                every { goalRepository.findById(GoalId(99L)) } returns null

                Then("EntityNotFoundException이 발생한다") {
                    shouldThrow<EntityNotFoundException> {
                        goalService.delete(command, memberId)
                    }
                }
            }

            When("본인 목표가 아니면") {
                val command = GoalCommand.Delete(goalId = GoalId(1L))
                every { goalRepository.findById(GoalId(1L)) } returns savedGoal

                Then("AccessDeniedException이 발생한다") {
                    shouldThrow<AccessDeniedException> {
                        goalService.delete(command, otherMemberId)
                    }
                }
            }
        }

        val date = LocalDate(2026, 3, 24)
        val savedDailyGoal =
            DailyGoal(
                id = DailyGoalId(1L),
                goalId = GoalId(1L),
                memberId = memberId,
                date = date,
                createdAt = Instant.now(),
            )

        Given("일별 목표 추가 시") {
            When("유효한 입력이면") {
                val command = DailyGoalCommand.Add(goalId = GoalId(1L), memberId = memberId, date = date)
                every { goalRepository.findById(GoalId(1L)) } returns savedGoal
                every { dailyGoalRepository.existsByGoalIdAndMemberIdAndDate(GoalId(1L), memberId, date) } returns false
                every { dailyGoalRepository.save(command) } returns savedDailyGoal

                val result = goalService.addDailyGoal(command)

                Then("해당 목표를 반환한다") {
                    result.id.value shouldBe 1L
                    result.title.value shouldBe "영어 공부"
                }
            }

            When("존재하지 않는 목표이면") {
                val command = DailyGoalCommand.Add(goalId = GoalId(99L), memberId = memberId, date = date)
                every { goalRepository.findById(GoalId(99L)) } returns null

                Then("EntityNotFoundException이 발생한다") {
                    shouldThrow<EntityNotFoundException> {
                        goalService.addDailyGoal(command)
                    }
                }
            }

            When("본인 목표가 아니면") {
                val command = DailyGoalCommand.Add(goalId = GoalId(1L), memberId = otherMemberId, date = date)
                every { goalRepository.findById(GoalId(1L)) } returns savedGoal

                Then("AccessDeniedException이 발생한다") {
                    shouldThrow<AccessDeniedException> {
                        goalService.addDailyGoal(command)
                    }
                }
            }

            When("이미 같은 날짜에 같은 목표가 추가되어 있으면") {
                val command = DailyGoalCommand.Add(goalId = GoalId(1L), memberId = memberId, date = date)
                every { goalRepository.findById(GoalId(1L)) } returns savedGoal
                every { dailyGoalRepository.existsByGoalIdAndMemberIdAndDate(GoalId(1L), memberId, date) } returns true

                Then("DuplicateEntityException이 발생한다") {
                    shouldThrow<DuplicateEntityException> {
                        goalService.addDailyGoal(command)
                    }
                }
            }
        }

        Given("일별 목표 제거 시") {
            When("해당 날짜에 목표가 배치되어 있으면") {
                val command = DailyGoalCommand.Remove(goalId = GoalId(1L), memberId = memberId, date = date)
                every { dailyGoalRepository.existsByGoalIdAndMemberIdAndDate(GoalId(1L), memberId, date) } returns true
                justRun { dailyGoalRepository.delete(command) }

                goalService.removeDailyGoal(command)

                Then("삭제가 수행된다") {
                    verify { dailyGoalRepository.delete(command) }
                }

                Then("DailyGoalRemovedEvent가 발행된다") {
                    val eventSlot = slot<DailyGoalRemovedEvent>()
                    verify { eventPublisher.publishEvent(capture(eventSlot)) }
                    eventSlot.captured.goalId shouldBe GoalId(1L)
                    eventSlot.captured.date shouldBe date
                }
            }

            When("해당 날짜에 목표가 배치되어 있지 않으면") {
                val command = DailyGoalCommand.Remove(goalId = GoalId(99L), memberId = memberId, date = date)
                every { dailyGoalRepository.existsByGoalIdAndMemberIdAndDate(GoalId(99L), memberId, date) } returns
                    false

                Then("EntityNotFoundException이 발생한다") {
                    shouldThrow<EntityNotFoundException> {
                        goalService.removeDailyGoal(command)
                    }
                }
            }
        }

        Given("assignedDate 필터로 목표 조회 시") {
            When("해당 날짜에 배치된 목표가 있으면") {
                val query = GoalQuery(memberId = memberId, assignedDate = date)
                every { goalRepository.findAll(query) } returns listOf(savedGoal)

                val result = goalService.findAll(query)

                Then("배치된 목표만 반환한다") {
                    result shouldHaveSize 1
                    result[0].title.value shouldBe "영어 공부"
                }
            }

            When("해당 날짜에 배치된 목표가 없으면") {
                val query = GoalQuery(memberId = memberId, assignedDate = LocalDate(2026, 1, 1))
                every { goalRepository.findAll(query) } returns emptyList()

                val result = goalService.findAll(query)

                Then("빈 목록을 반환한다") {
                    result shouldHaveSize 0
                }
            }
        }
    })
