package kr.io.team.loop.auth.presentation.datafetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.io.team.loop.auth.application.service.AuthService
import kr.io.team.loop.auth.domain.model.LoginId
import kr.io.team.loop.auth.domain.model.Member
import kr.io.team.loop.auth.domain.model.MemberCommand
import kr.io.team.loop.auth.domain.model.Nickname
import kr.io.team.loop.codegen.types.AuthToken
import kr.io.team.loop.codegen.types.LoginInput
import kr.io.team.loop.codegen.types.RegisterInput
import kr.io.team.loop.common.config.Authorize
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.codegen.types.Member as MemberGraphql

@DgsComponent
class AuthDataFetcher(
    private val authService: AuthService,
) {
    @DgsQuery
    fun me(
        @Authorize memberId: Long,
    ): MemberGraphql = authService.getMe(MemberId(memberId)).toGraphql()

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
        val command =
            MemberCommand.Login(
                loginId = LoginId(input.loginId),
                rawPassword = input.password,
            )
        val result = authService.login(command)
        return AuthToken(accessToken = result.accessToken)
    }

    private fun Member.toGraphql(): MemberGraphql =
        MemberGraphql(
            id = id.value.toString(),
            loginId = loginId.value,
            nickname = nickname.value,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt?.toString(),
        )
}
