package kr.io.team.loop.auth.application.service

import kr.io.team.loop.auth.application.dto.AuthTokenDto
import kr.io.team.loop.auth.domain.model.LoginId
import kr.io.team.loop.auth.domain.model.MemberCommand
import kr.io.team.loop.auth.domain.repository.MemberRepository
import kr.io.team.loop.common.config.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @Transactional
    fun register(command: MemberCommand.Register): AuthTokenDto {
        require(!memberRepository.existsByLoginId(command.loginId)) {
            "LoginId already exists: ${command.loginId.value}"
        }
        val encodedPassword = checkNotNull(passwordEncoder.encode(command.rawPassword))
        val member = memberRepository.save(command, encodedPassword)
        val token = jwtTokenProvider.generateToken(member.id.value)
        return AuthTokenDto(accessToken = token)
    }

    @Transactional(readOnly = true)
    fun login(
        loginId: LoginId,
        rawPassword: String,
    ): AuthTokenDto {
        val member =
            memberRepository.findByLoginId(loginId)
                ?: throw NoSuchElementException("Member not found: ${loginId.value}")
        require(passwordEncoder.matches(rawPassword, member.password)) {
            "Password does not match"
        }
        val token = jwtTokenProvider.generateToken(member.id.value)
        return AuthTokenDto(accessToken = token)
    }
}
