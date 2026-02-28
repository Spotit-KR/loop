package kr.io.team.loop.auth.presentation.datafetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.InputArgument
import kr.io.team.loop.auth.application.service.AuthService
import kr.io.team.loop.auth.domain.model.LoginId
import kr.io.team.loop.auth.domain.model.MemberCommand
import kr.io.team.loop.auth.domain.model.Nickname
import kr.io.team.loop.codegen.types.AuthToken
import kr.io.team.loop.codegen.types.LoginInput
import kr.io.team.loop.codegen.types.RegisterInput

@DgsComponent
class AuthDataFetcher(
    private val authService: AuthService,
) {
    @DgsMutation
    fun register(
        @InputArgument input: RegisterInput,
    ): AuthToken {
        val command =
            MemberCommand.Register(
                loginId = LoginId(input.loginId),
                nickname = Nickname(input.nickname),
                rawPassword = input.password,
            )
        val result = authService.register(command)
        return AuthToken(accessToken = result.accessToken)
    }

    @DgsMutation
    fun login(
        @InputArgument input: LoginInput,
    ): AuthToken {
        val result =
            authService.login(
                loginId = LoginId(input.loginId),
                rawPassword = input.password,
            )
        return AuthToken(accessToken = result.accessToken)
    }
}
