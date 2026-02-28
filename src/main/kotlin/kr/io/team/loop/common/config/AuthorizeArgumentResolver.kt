package kr.io.team.loop.common.config

import com.netflix.graphql.dgs.context.DgsContext
import com.netflix.graphql.dgs.internal.method.ArgumentResolver
import graphql.schema.DataFetchingEnvironment
import org.springframework.core.MethodParameter
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
class AuthorizeArgumentResolver(
    private val jwtTokenProvider: JwtTokenProvider,
) : ArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(Authorize::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        dfe: DataFetchingEnvironment,
    ): Any? {
        val authorize = parameter.getParameterAnnotation(Authorize::class.java)!!
        val token = extractToken(dfe)

        if (token != null && jwtTokenProvider.validateToken(token)) {
            return jwtTokenProvider.getMemberIdFromToken(token)
        }

        if (authorize.require) {
            throw IllegalArgumentException("Authentication required")
        }
        return null
    }

    private fun extractToken(dfe: DataFetchingEnvironment): String? {
        val authHeader =
            DgsContext
                .getRequestData(dfe)
                ?.headers
                ?.getFirst("Authorization")
                ?: return null
        return authHeader.removePrefix("Bearer ").takeIf { it != authHeader }
    }
}
