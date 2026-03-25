package kr.io.team.loop.goal.application.service

import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.event.DailyGoalRemovedEvent
import kr.io.team.loop.common.domain.event.GoalDeletedEvent
import kr.io.team.loop.common.domain.exception.AccessDeniedException
import kr.io.team.loop.common.domain.exception.DuplicateEntityException
import kr.io.team.loop.common.domain.exception.EntityNotFoundException
import kr.io.team.loop.goal.domain.model.DailyGoalCommand
import kr.io.team.loop.goal.domain.model.Goal
import kr.io.team.loop.goal.domain.model.GoalCommand
import kr.io.team.loop.goal.domain.model.GoalQuery
import kr.io.team.loop.goal.domain.repository.DailyGoalRepository
import kr.io.team.loop.goal.domain.repository.GoalRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GoalService(
    private val goalRepository: GoalRepository,
    private val dailyGoalRepository: DailyGoalRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun create(command: GoalCommand.Create): Goal = goalRepository.save(command)

    @Transactional(readOnly = true)
    fun findAll(query: GoalQuery): List<Goal> = goalRepository.findAll(query)

    @Transactional(readOnly = true)
    fun findById(id: GoalId): Goal =
        goalRepository.findById(id)
            ?: throw EntityNotFoundException("Goal not found: ${id.value}")

    @Transactional
    fun update(
        command: GoalCommand.Update,
        memberId: MemberId,
    ): Goal {
        val goal =
            goalRepository.findById(command.goalId)
                ?: throw EntityNotFoundException("Goal not found: ${command.goalId.value}")
        if (!goal.isOwnedBy(memberId)) {
            throw AccessDeniedException("Goal does not belong to member: ${memberId.value}")
        }
        return goalRepository.update(command)
    }

    @Transactional
    fun delete(
        command: GoalCommand.Delete,
        memberId: MemberId,
    ) {
        val goal =
            goalRepository.findById(command.goalId)
                ?: throw EntityNotFoundException("Goal not found: ${command.goalId.value}")
        if (!goal.isOwnedBy(memberId)) {
            throw AccessDeniedException("Goal does not belong to member: ${memberId.value}")
        }
        dailyGoalRepository.deleteByGoalId(command.goalId)
        goalRepository.delete(command)
        eventPublisher.publishEvent(GoalDeletedEvent(command.goalId))
    }

    @Transactional
    fun addDailyGoal(command: DailyGoalCommand.Add): Goal {
        val goal =
            goalRepository.findById(command.goalId)
                ?: throw EntityNotFoundException("Goal not found: ${command.goalId.value}")
        if (!goal.isOwnedBy(command.memberId)) {
            throw AccessDeniedException("Goal does not belong to member: ${command.memberId.value}")
        }
        if (dailyGoalRepository.existsByGoalIdAndMemberIdAndDate(command.goalId, command.memberId, command.date)) {
            throw DuplicateEntityException(
                "DailyGoal already exists for goal ${command.goalId.value} on ${command.date}",
            )
        }
        dailyGoalRepository.save(command)
        return goal
    }

    @Transactional
    fun removeDailyGoal(command: DailyGoalCommand.Remove) {
        if (!dailyGoalRepository.existsByGoalIdAndMemberIdAndDate(command.goalId, command.memberId, command.date)) {
            throw EntityNotFoundException(
                "DailyGoal not found for goal ${command.goalId.value} on ${command.date}",
            )
        }
        dailyGoalRepository.delete(command)
        eventPublisher.publishEvent(DailyGoalRemovedEvent(command.goalId, command.memberId, command.date))
    }
}
