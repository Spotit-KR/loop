package kr.io.team.loop.task.domain.model

import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.TaskId

data class TaskQuery(
    val taskId: TaskId? = null,
    val memberId: MemberId? = null,
    val taskDate: TaskDate? = null,
)
