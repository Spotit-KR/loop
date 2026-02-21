package kr.io.team.loop.auth.presentation.controller

import kr.io.team.loop.auth.application.service.LoginService
import kr.io.team.loop.auth.application.service.LogoutService
import kr.io.team.loop.auth.application.service.RefreshService
import kr.io.team.loop.auth.application.service.RegisterService
import kr.io.team.loop.auth.domain.model.AuthCommand
import kr.io.team.loop.auth.domain.model.Password
import kr.io.team.loop.auth.domain.model.RefreshToken
import kr.io.team.loop.auth.presentation.request.LoginRequest
import kr.io.team.loop.auth.presentation.request.LogoutRequest
import kr.io.team.loop.auth.presentation.request.RefreshRequest
import kr.io.team.loop.auth.presentation.request.RegisterRequest
import kr.io.team.loop.auth.presentation.response.AuthTokenResponse
import kr.io.team.loop.common.domain.Email
import kr.io.team.loop.common.domain.Nickname
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val registerService: RegisterService,
    private val loginService: LoginService,
    private val refreshService: RefreshService,
    private val logoutService: LogoutService,
) {
    @PostMapping("/register")
    fun register(
        @RequestBody request: RegisterRequest,
    ): ResponseEntity<AuthTokenResponse> {
        val command =
            AuthCommand.Register(
                email = Email(request.email),
                password = Password(request.password),
                nickname = Nickname(request.nickname),
            )
        val dto = registerService.execute(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthTokenResponse.from(dto))
    }

    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
    ): ResponseEntity<AuthTokenResponse> {
        val command =
            AuthCommand.Login(
                email = Email(request.email),
                password = Password(request.password),
            )
        val dto = loginService.execute(command)
        return ResponseEntity.ok(AuthTokenResponse.from(dto))
    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody request: RefreshRequest,
    ): ResponseEntity<AuthTokenResponse> {
        val command = AuthCommand.Refresh(refreshToken = RefreshToken(request.refreshToken))
        val dto = refreshService.execute(command)
        return ResponseEntity.ok(AuthTokenResponse.from(dto))
    }

    @PostMapping("/logout")
    fun logout(
        @RequestBody request: LogoutRequest,
    ): ResponseEntity<Unit> {
        val command = AuthCommand.Logout(refreshToken = RefreshToken(request.refreshToken))
        logoutService.execute(command)
        return ResponseEntity.noContent().build()
    }
}
