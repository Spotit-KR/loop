package kr.io.team.loop.common.config

import com.netflix.graphql.types.errors.ErrorType
import graphql.GraphQLError
import kr.io.team.loop.common.domain.exception.AccessDeniedException
import kr.io.team.loop.common.domain.exception.AuthenticationException
import kr.io.team.loop.common.domain.exception.BusinessRuleException
import kr.io.team.loop.common.domain.exception.DuplicateEntityException
import kr.io.team.loop.common.domain.exception.EntityNotFoundException
import kr.io.team.loop.common.domain.exception.InvalidInputException
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler
import org.springframework.web.bind.annotation.ControllerAdvice

@ControllerAdvice
class GraphQlExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @GraphQlExceptionHandler
    fun handleInvalidInput(ex: InvalidInputException): GraphQLError {
        log.warn("[BAD_REQUEST] {}", ex.message, ex)
        return GraphQLError
            .newError()
            .errorType(ErrorType.BAD_REQUEST)
            .message(ex.message)
            .build()
    }

    @GraphQlExceptionHandler
    fun handleEntityNotFound(ex: EntityNotFoundException): GraphQLError {
        log.warn("[NOT_FOUND] {}", ex.message, ex)
        return GraphQLError
            .newError()
            .errorType(ErrorType.NOT_FOUND)
            .message(ex.message)
            .build()
    }

    @GraphQlExceptionHandler
    fun handleDuplicateEntity(ex: DuplicateEntityException): GraphQLError {
        log.warn("[FAILED_PRECONDITION] {}", ex.message, ex)
        return GraphQLError
            .newError()
            .errorType(ErrorType.FAILED_PRECONDITION)
            .message(ex.message)
            .build()
    }

    @GraphQlExceptionHandler
    fun handleBusinessRule(ex: BusinessRuleException): GraphQLError {
        log.warn("[FAILED_PRECONDITION] {}", ex.message, ex)
        return GraphQLError
            .newError()
            .errorType(ErrorType.FAILED_PRECONDITION)
            .message(ex.message)
            .build()
    }

    @GraphQlExceptionHandler
    fun handleAuthentication(ex: AuthenticationException): GraphQLError {
        log.warn("[UNAUTHENTICATED] {}", ex.message, ex)
        return GraphQLError
            .newError()
            .errorType(ErrorType.UNAUTHENTICATED)
            .message(ex.message)
            .build()
    }

    @GraphQlExceptionHandler
    fun handleAccessDenied(ex: AccessDeniedException): GraphQLError {
        log.warn("[PERMISSION_DENIED] {}", ex.message, ex)
        return GraphQLError
            .newError()
            .errorType(ErrorType.PERMISSION_DENIED)
            .message(ex.message)
            .build()
    }
}
