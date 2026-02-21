package kr.io.team.loop.task.domain.model

@JvmInline
value class TaskTitle(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "TaskTitle must not be blank" }
        require(value.length <= 200) { "TaskTitle must not exceed 200 characters" }
    }
}
