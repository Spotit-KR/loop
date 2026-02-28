package kr.io.team.loop.common.domain.exception

sealed class LoopException(
    override val message: String,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)

class InvalidInputException(
    message: String,
    cause: Throwable? = null,
) : LoopException(message, cause)

class EntityNotFoundException(
    message: String,
) : LoopException(message)

class DuplicateEntityException(
    message: String,
) : LoopException(message)

class BusinessRuleException(
    message: String,
) : LoopException(message)

class AuthenticationException(
    message: String,
    cause: Throwable? = null,
) : LoopException(message, cause)

class AccessDeniedException(
    message: String,
) : LoopException(message)
