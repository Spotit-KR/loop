# Sprint 1-1 Member BC 구현 문서

## 구현 파일 목록

### Domain Layer (`member/domain/`)

| 파일 | 역할 |
|------|------|
| `model/Email.kt` | 이메일 VO — 정규식 + 255자 검증 |
| `model/Password.kt` | 평문 비밀번호 VO — 8~100자 검증 |
| `model/HashedPassword.kt` | 해시 비밀번호 VO — 공백 불허 검증 |
| `model/Nickname.kt` | 닉네임 VO — 공백 불허 + 50자 검증 |
| `model/ProfileImageUrl.kt` | 프로필 이미지 URL VO — 공백 불허 + 500자 검증 |
| `model/Member.kt` | Member 엔티티 (data class) |
| `model/MemberCommand.kt` | sealed interface — Register, UpdateProfile |
| `model/MemberQuery.kt` | 조회 조건 data class (nullable 필드) |
| `repository/MemberRepository.kt` | 저장소 인터페이스 |

### Application Layer (`member/application/`)

| 파일 | 역할 |
|------|------|
| `dto/MemberDto.kt` | 서비스 반환 DTO (`from(Member)` 팩토리) |
| `service/RegisterMemberService.kt` | 회원 등록 — 이메일 중복 검사 후 저장 |
| `service/GetMemberService.kt` | 회원 조회 — 미존재 시 NoSuchElementException |
| `service/UpdateMemberProfileService.kt` | 프로필 업데이트 — 미존재 시 NoSuchElementException |

### Infrastructure Layer (`member/infrastructure/`)

| 파일 | 역할 |
|------|------|
| `persistence/MembersTable.kt` | Exposed v1 LongIdTable 정의 |
| `persistence/ExposedMemberRepository.kt` | MemberRepository 구현체 — CRUD 및 toMember() 매핑 |

### Presentation Layer (`member/presentation/` + `common/`)

| 파일 | 역할 |
|------|------|
| `common/config/CurrentMemberId.kt` | `@CurrentMemberId` 파라미터 어노테이션 |
| `common/config/CurrentMemberIdArgumentResolver.kt` | SecurityContext에서 memberId 추출 (stub) |
| `common/config/WebMvcConfig.kt` | resolver 등록 추가 |
| `presentation/request/UpdateMemberProfileRequest.kt` | PUT /members/me 요청 DTO |
| `presentation/response/MemberResponse.kt` | 응답 DTO (`from(MemberDto)` 팩토리) |
| `presentation/controller/MemberController.kt` | GET /members/me, PUT /members/me |

---

## 주요 설계 내용

### Value Objects

모두 `@JvmInline value class` + `init` 블록 검증:

```kotlin
@JvmInline
value class Email(val value: String) {
    init {
        require(value.matches(EMAIL_REGEX)) { "Invalid email format" }
        require(value.length <= 255) { "Email must not exceed 255 characters" }
    }
}
```

`Password`(평문)와 `HashedPassword`(해시)를 분리해 도메인 의도를 명확히 표현.

### Command / Query

```kotlin
sealed interface MemberCommand {
    data class Register(val email: Email, val hashedPassword: HashedPassword, val nickname: Nickname) : MemberCommand
    data class UpdateProfile(val memberId: MemberId, val nickname: Nickname, val profileImageUrl: ProfileImageUrl?) : MemberCommand
}

data class MemberQuery(val memberId: MemberId? = null, val email: Email? = null)
```

### Service 패턴

UseCase 인터페이스 없이 Service 클래스가 직접 구현. 이메일 중복/존재 여부를 service 레이어에서 검증:

```kotlin
@Service
class RegisterMemberService(private val memberRepository: MemberRepository) {
    @Transactional
    fun execute(command: MemberCommand.Register): MemberDto {
        require(memberRepository.findByEmail(command.email) == null) { "Email already exists" }
        return MemberDto.from(memberRepository.save(command))
    }
}
```

### Exposed v1 특이사항

Exposed 1.0.0은 패키지 구조가 완전히 변경됨:

| 구분 | 이전 (0.x) | 현재 (1.0.0) |
|------|-----------|-------------|
| LongIdTable | `org.jetbrains.exposed.sql.Table` | `org.jetbrains.exposed.v1.core.dao.id.LongIdTable` |
| datetime() | `org.jetbrains.exposed.sql.kotlin.datetime.datetime` | `org.jetbrains.exposed.v1.datetime.datetime` |
| selectAll() | `org.jetbrains.exposed.sql.selectAll` | `org.jetbrains.exposed.v1.jdbc.selectAll` |
| insertAndGetId() | `org.jetbrains.exposed.sql.insertAndGetId` | `org.jetbrains.exposed.v1.jdbc.insertAndGetId` |
| update() | `org.jetbrains.exposed.sql.update` | `org.jetbrains.exposed.v1.jdbc.update` |
| andWhere() | `org.jetbrains.exposed.sql.andWhere` | `org.jetbrains.exposed.v1.jdbc.andWhere` |
| ResultRow | `org.jetbrains.exposed.sql.ResultRow` | `org.jetbrains.exposed.v1.core.ResultRow` |
| eq | `org.jetbrains.exposed.sql.SqlExpressionBuilder.eq` | `org.jetbrains.exposed.v1.core.eq` |
| EntityID | `org.jetbrains.exposed.dao.id.EntityID` | `org.jetbrains.exposed.v1.core.dao.id.EntityID` |

EntityID 컬럼(id) 비교 시 `EntityID` 래핑 필요:
```kotlin
val entityId = EntityID(id.value, MembersTable)
MembersTable.selectAll().where { MembersTable.id eq entityId }
```

---

## TDD 테스트 목록

### Domain Layer

**EmailTest** (4 케이스)
- ✅ 유효한 이메일 → 생성 성공
- ✅ 빈 문자열 → IllegalArgumentException
- ✅ @ 없는 문자열 → IllegalArgumentException
- ✅ 255자 초과 → IllegalArgumentException

**PasswordTest** (3 케이스)
- ✅ 8자 이상 → 생성 성공
- ✅ 7자 이하 → IllegalArgumentException
- ✅ 100자 초과 → IllegalArgumentException

**NicknameTest** (3 케이스)
- ✅ 유효한 닉네임 → 생성 성공
- ✅ 빈 문자열 → IllegalArgumentException
- ✅ 50자 초과 → IllegalArgumentException

**MemberTest** (2 케이스)
- ✅ 유효한 데이터 → 모든 필드 정상
- ✅ profileImageUrl null → profileImageUrl null

### Application Layer

**RegisterMemberServiceTest** (2 케이스)
- ✅ 유효한 커맨드 → MemberDto 반환
- ✅ 이미 존재하는 이메일 → IllegalArgumentException

**GetMemberServiceTest** (2 케이스)
- ✅ 존재하는 memberId → MemberDto 반환
- ✅ 존재하지 않는 memberId → NoSuchElementException

**UpdateMemberProfileServiceTest** (2 케이스)
- ✅ 존재하는 회원 → 업데이트된 MemberDto 반환
- ✅ 존재하지 않는 memberId → NoSuchElementException

---

## 빌드/테스트 결과

```
./gradlew test  → BUILD SUCCESSFUL (16 tests, 0 failures)
./gradlew build → BUILD SUCCESSFUL (ktlint 포함)
```
