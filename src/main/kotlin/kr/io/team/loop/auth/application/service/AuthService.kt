package kr.io.team.loop.auth.application.service

import kr.io.team.loop.auth.application.dto.AuthTokenDto
import kr.io.team.loop.auth.domain.model.LoginId
import kr.io.team.loop.auth.domain.model.MemberCommand
import kr.io.team.loop.auth.domain.repository.MemberRepository
import kr.io.team.loop.common.config.JwtTokenProvider
import kr.io.team.loop.common.domain.exception.AuthenticationException
import kr.io.team.loop.common.domain.exception.DuplicateEntityException
import kr.io.team.loop.common.domain.exception.EntityNotFoundException
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
        if (memberRepository.existsByLoginId(command.loginId)) {
            throw DuplicateEntityException("LoginId already exists: ${command.loginId.value}")
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
                ?: throw EntityNotFoundException("Member not found: ${loginId.value}")
        if (!passwordEncoder.matches(rawPassword, member.password)) {
            throw AuthenticationException("Password does not match")
        }
        val token = jwtTokenProvider.generateToken(member.id.value)
        return AuthTokenDto(accessToken = token)
    }
}
