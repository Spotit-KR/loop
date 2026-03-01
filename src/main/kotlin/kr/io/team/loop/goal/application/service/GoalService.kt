package kr.io.team.loop.goal.application.service

import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.exception.AccessDeniedException
import kr.io.team.loop.common.domain.exception.EntityNotFoundException
import kr.io.team.loop.goal.domain.model.Goal
import kr.io.team.loop.goal.domain.model.GoalCommand
import kr.io.team.loop.goal.domain.model.GoalQuery
import kr.io.team.loop.goal.domain.repository.GoalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GoalService(
    private val goalRepository: GoalRepository,
) {
    @Transactional
    fun create(command: GoalCommand.Create): Goal = goalRepository.save(command)

    @Transactional(readOnly = true)
    fun findAll(query: GoalQuery): List<Goal> = goalRepository.findAll(query)

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
        goalRepository.delete(command)
    }
}
