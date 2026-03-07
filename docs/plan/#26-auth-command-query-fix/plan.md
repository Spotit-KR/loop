# auth BC: Command/Query 패턴 미적용 수정 계획

> Issue: #26

## 단계

- [x] 1단계: Domain — MemberCommand에 Login variant 추가, Register에 encodedPassword 포함 (TDD)
- [x] 2단계: Domain — MemberRepository.save() 시그니처에서 encodedPassword 파라미터 제거
- [x] 3단계: Application — AuthService.login()이 MemberCommand.Login을 수신하도록 변경, register()에서 command.copy로 encodedPassword 전달 (TDD)
- [x] 4단계: Infrastructure — ExposedMemberRepository.save() 구현 수정
- [x] 5단계: Presentation — AuthDataFetcher.login()에서 MemberCommand.Login 변환
- [x] 6단계: 전체 테스트 통과 검증
