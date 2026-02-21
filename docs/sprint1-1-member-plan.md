# Sprint 1-1: Member BC 구현 계획

## 개요

Member BC의 구현 계획서. member-planner(opus)가 수립, 리더가 승인.

## 파일 구조

### Domain Layer (`member/domain/`)
- `model/Email.kt` — 이메일 VO (→ 추후 common/domain/으로 이동 예정)
- `model/Password.kt` — 원본 비밀번호 VO (유효성 검증용)
- `model/HashedPassword.kt` — 해싱된 비밀번호 VO (엔티티 저장용)
- `model/Nickname.kt` — 닉네임 VO (→ 추후 common/domain/으로 이동 예정)
- `model/ProfileImageUrl.kt` — 프로필 이미지 URL VO
- `model/Member.kt` — Member 엔티티
- `model/MemberCommand.kt` — Command sealed interface (Register, UpdateProfile)
- `model/MemberQuery.kt` — Query data class
- `repository/MemberRepository.kt` — Repository 인터페이스

### Application Layer (`member/application/`)
- `dto/MemberDto.kt` — 서비스 반환 DTO
- `service/RegisterMemberService.kt` — 회원 등록
- `service/GetMemberService.kt` — 회원 조회
- `service/UpdateMemberProfileService.kt` — 프로필 수정

### Infrastructure Layer (`member/infrastructure/`)
- `persistence/MembersTable.kt` — Exposed Table 정의
- `persistence/ExposedMemberRepository.kt` — Repository 구현체

### Presentation Layer (`member/presentation/`)
- `request/UpdateMemberProfileRequest.kt` — 프로필 수정 요청 DTO
- `response/MemberResponse.kt` — 회원 응답 DTO
- `controller/MemberController.kt` — REST Controller (GET/PUT /members/me)

## 주요 설계 결정

| 결정 | 내용 |
|------|------|
| Password 분리 | Password(raw) + HashedPassword(hashed) 분리 |
| Email/Nickname | 추후 common/domain/으로 이동 (Auth BC 공유) |
| BC 간 의존 | Auth → Member 단방향 의존 허용 |
| @CurrentMemberId | common에 stub resolver 생성, Auth BC에서 실제 구현 |

## TDD 테스트 대상

- Domain: EmailTest, PasswordTest, NicknameTest, MemberTest
- Application: RegisterMemberServiceTest, GetMemberServiceTest, UpdateMemberProfileServiceTest
