# 로그인 회원 본인 정보 조회 (me) Query 구현

> Issue: #36

## 단계

- [x] 1단계: GraphQL 스키마에 Member 타입과 me Query 추가
- [x] 2단계: DGS Codegen 실행하여 Member 타입 생성
- [x] 3단계: Domain — MemberRepository에 findById 메서드 추가 (TDD)
- [x] 4단계: Application — AuthService에 getMe 메서드 추가 (TDD)
- [x] 5단계: Infrastructure — ExposedMemberRepository에 findById 구현
- [x] 6단계: Presentation — AuthDataFetcher에 me Query 추가
- [x] 7단계: 전체 테스트 통과 확인
