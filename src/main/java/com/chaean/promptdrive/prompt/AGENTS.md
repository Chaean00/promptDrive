# prompt 모듈 작업 지침

루트 `AGENTS.md`의 공통 지침을 우선 적용하며, 이 문서는 `prompt` 모듈에만 추가로 적용합니다.

## 책임 범위

- Prompt Entity, 고정 카테고리, 공개 카탈로그, 관리자 큐레이션 Prompt, 사용자 커뮤니티 Prompt를 소유합니다.
- Prompt 좋아요처럼 Prompt 상태와 직접 연결되는 기능은 이 모듈의 책임으로 판단합니다.
- 외부 사용자가 접근하는 API는 `internal/web`, 비즈니스 흐름은 `internal/application`, 저장 모델과 Repository는 `internal/persistence`에 둡니다.

## 폴더 역할

- `api/facade`: 다른 모듈이 Prompt의 공개 상태나 최소한의 Prompt 정보를 동기적으로 확인할 때 사용하는 Facade를 둡니다.
- `api/dto`: 다른 모듈과 공유해야 하는 안정적인 Prompt 계약 DTO와 이벤트 payload DTO를 둡니다.
- `internal/adapter`: 외부 시스템이나 파일·Import 같은 교체 가능한 경계가 실제로 필요할 때만 둡니다.
- `internal/application/catalog`: 공개 카탈로그 조회와 관리자 큐레이션 Prompt 유스케이스를 둡니다.
- `internal/application/community`: 일반 사용자의 Prompt 생성·조회·수정·삭제 유스케이스를 둡니다.
- `internal/application/like`: Prompt 좋아요 등록·취소와 중복 방지 유스케이스를 둡니다.
- `internal/application/ranking`: Prompt 좋아요 수를 기반으로 한 랭킹 조회 유스케이스를 둡니다. 별도 `ranking` 모듈로 분리할 필요가 생기면 공개 계약을 유지한 채 이동합니다.
- `internal/domain`: Prompt provenance, visibility, category 같은 핵심 규칙과 도메인 타입을 둡니다.
- `internal/persistence`: Prompt, PromptCategory, PromptLike Entity와 Repository를 둡니다.
- `internal/web/catalog`: 공개 카탈로그와 관리자 큐레이션 HTTP 진입점을 둡니다.
- `internal/web/community`: 사용자 Prompt 관리 HTTP 진입점을 둡니다.
- `internal/web/like`: 좋아요 등록·취소 HTTP 진입점을 둡니다.
- `internal/web/ranking`: 랭킹 조회 HTTP 진입점을 둡니다.
- `internal/dto`: prompt 모듈 내부 Controller·Service에서만 사용하는 Request/Response DTO를 둡니다.

현재 없는 폴더도 해당 책임의 기능을 구현할 때만 생성하며, 한 번만 사용하는 계층이나 빈 패키지는 미리 만들지 않습니다.

## 구현 규칙

- 관리자 큐레이션과 사용자 커뮤니티 Prompt의 생성·수정·삭제 규칙을 서비스에서 명확히 분리합니다.
- Prompt의 `provenance`, `visibility`, `ownerMemberId` 불변식을 Entity와 서비스 양쪽의 적절한 경계에서 검증합니다.
- Prompt와 카테고리 조회는 기존 Repository와 응답 매퍼를 우선 재사용합니다.
- DTO는 `internal/dto`에 별도 파일로 만들고 `*Request`, `*Response` 명명 규칙을 지킵니다. Controller 내부 DTO를 만들지 않습니다.
- 다른 모듈의 내부 Entity, Repository, Service를 import하지 않습니다. member ID는 숫자 스칼라로 취급합니다.
- 모듈 간 통신이 필요하면 반드시 `prompt/api`의 공개 Facade와 DTO를 통해 통신합니다.
- 다른 모듈은 `prompt/internal`의 Entity, Repository, Service, Web, 내부 DTO를 직접 import하지 않습니다.
- Prompt 관련 후속 처리는 이벤트를 우선 검토하고, 동기 검증이 필요한 경우에만 최소 범위의 공개 Facade를 사용합니다.
- `/api`는 구현 위치가 아니라 모듈 간 공개 계약 위치입니다. 구현체는 반드시 `internal/application`에 둡니다.
- soft delete 대상은 `deleted_at IS NULL` 조회 규칙을 유지하고, 삭제된 Prompt·카테고리·좋아요가 공개 조회에 노출되지 않도록 합니다.
- 기존 스키마와 인덱스를 먼저 확인합니다. 요구사항 없이 새 migration·인덱스·FK를 추가하지 않습니다.

## 좋아요 및 랭킹

- 기존 `prompt_like` 테이블과 활성 좋아요 중복 제약을 우선 재사용합니다.
- 좋아요 등록·취소는 멱등성과 인증된 member ID를 기준으로 구현합니다.
- 공개 Prompt만 랭킹 대상에 포함하고, 좋아요 수가 같을 때는 결정적인 보조 정렬 기준을 사용합니다.
- 랭킹 조회가 별도 모듈로 확장될 때도 `prompt/internal` 구현을 직접 노출하지 않고 최소 공개 계약을 사용합니다.

## 검증

- Prompt 비즈니스 규칙은 서비스 단위 테스트로 검증합니다.
- 소유자·공개 여부·삭제·좋아요 중복처럼 클라이언트에 영향을 주는 경계는 HTTP/MySQL 통합 테스트로 검증합니다.
- 단순 DTO, Lombok 생성 코드, migration의 열·인덱스 존재만을 위한 테스트는 추가하지 않습니다.
