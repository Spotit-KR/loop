package kr.io.team.loop.common.domain

@JvmInline
value class TaskId(
    val value: Long,
) {
    init {
        require(value > 0) { "TaskId must be positive" }
    }
}
