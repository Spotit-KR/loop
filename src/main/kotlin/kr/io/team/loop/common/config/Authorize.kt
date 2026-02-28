package kr.io.team.loop.common.config

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Authorize(
    val require: Boolean = true,
)
