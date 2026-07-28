# member 모듈 작업 지침

루트 `AGENTS.md`의 공통 지침을 우선 적용하며, 이 문서는 `member` 모듈에만 추가로 적용합니다.

## 책임 범위

- 회원 식별, 소셜 아이덴티티, OAuth 로그인, 애플리케이션 토큰과 인증 수명 주기를 소유합니다.
- Google·Kakao 같은 외부 OAuth 제공자 연동은 `internal/adapter`에 한정합니다.
- 회원 Entity, Repository, 인증 구현은 모듈 내부에 두고 다른 모듈에 직접 노출하지 않습니다.

## 폴더 역할

- `api/facade`: 다른 모듈이 회원 상태를 동기적으로 확인할 때 사용하는 최소 Facade 인터페이스를 둡니다.
- `api/dto`: 다른 모듈과 공유해야 하는 안정적인 회원·인증 계약 DTO를 둡니다. Entity나 내부 구현 타입은 두지 않습니다.
- `internal/adapter`: Google·Kakao 등 외부 OAuth provider와의 통신 및 외부 기술 변환을 둡니다.
- `internal/application`: 회원 가입, 로그인, 토큰 발급·갱신·폐기 같은 유스케이스 조합을 둡니다.
- `internal/domain`: 회원과 소셜 아이덴티티의 핵심 규칙과 도메인 타입을 둡니다.
- `internal/persistence`: 회원·소셜 아이덴티티 Entity와 Repository를 둡니다.
- `internal/web`: 로그인, callback, token, logout 등 외부 HTTP 진입점을 둡니다.
- `internal/dto`: member 모듈 내부 Controller·Service에서만 사용하는 Request/Response DTO를 둡니다.

현재 없는 폴더라도 위 책임에 해당하는 기능이 추가될 때만 생성합니다.

## 모듈 계약

- 모듈 간 통신이 필요하면 반드시 `member/api`의 공개 Facade와 DTO를 통해 통신합니다.
- 다른 모듈은 `member/internal`의 Entity, Repository, Service, Adapter, 내부 DTO를 직접 import하지 않습니다.
- 이벤트를 사용할 때도 `member/api`에 직렬화 가능한 이벤트 계약 DTO를 정의하고 Entity를 payload에 넣지 않습니다.
- 다른 모듈에는 필요한 최소한의 공개 Facade와 DTO만 제공합니다.
- 다른 모듈의 member ID는 스칼라 값으로 사용하며 member Entity에 대한 JPA 관계를 만들지 않습니다.
- 인증 상태가 필요한 동기 검증은 공개 Facade를 사용하고, 후속 처리는 공개 이벤트 계약을 우선 검토합니다.
- OAuth provider 응답, Entity, lazy proxy를 이벤트 payload나 외부 응답에 직접 넣지 않습니다.

## 보안 및 설정

- access token, refresh token, OAuth state·PKCE·redirect URI 검증을 기존 보안 계약과 일관되게 처리합니다.
- API 키, client secret, JWT 서명 키는 환경 변수로만 주입합니다.
- 여러 환경 설정은 `@ConfigurationProperties`로 묶고 `@Value`를 여러 곳에 흩어 사용하지 않습니다.
- 인증 실패는 기존 공통 `BusinessException`과 `ErrorCode`를 사용하며 프레임워크 예외를 외부 계약으로 직접 노출하지 않습니다.

## 검증

- OAuth·토큰 수명 주기는 단위 테스트와 실제 HTTP/MySQL 통합 테스트로 검증합니다.
- 외부 OAuth API 테스트는 고정 fixture 또는 명시적인 외부 API 테스트로 경계를 검증합니다.
- 단순 DTO·Lombok 생성 코드 자체를 테스트하지 않습니다.
