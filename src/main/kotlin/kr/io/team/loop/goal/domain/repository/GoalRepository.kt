package kr.io.team.loop.goal.domain.repository

import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.goal.domain.model.Goal
import kr.io.team.loop.goal.domain.model.GoalCommand
import kr.io.team.loop.goal.domain.model.GoalQuery

interface GoalRepository {
    fun save(command: GoalCommand.Create): Goal

    fun update(command: GoalCommand.Update): Goal

    fun delete(command: GoalCommand.Delete)

    fun findAll(query: GoalQuery): List<Goal>

    fun findById(id: GoalId): Goal?
}
