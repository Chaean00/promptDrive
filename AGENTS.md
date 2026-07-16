# AGENTS.md

일반적인 LLM 코딩 실수를 줄이기 위한 행동 지침입니다. 필요에 따라 프로젝트별 지침과 함께 사용합니다.

**트레이드오프:** 이 지침은 속도보다 신중함을 우선합니다. 사소한 작업에는 상황에 맞게 판단합니다.

## 프로젝트 구현 원칙

- YAGNI(You Aren't Gonna Need It)를 따른다. 현재 요구사항을 만족하는 최소 구현을 선택하며, 미래의 가능성만으로 추상화·설정·테이블·메시지 브로커를 추가하지 않는다.
- 모듈 간 결합도를 최소화한다. 다른 모듈의 내부 타입·테이블·Repository·Service에 의존하지 않고, 필요한 경우에만 공개 이벤트 또는 최소 범위의 Facade 계약을 사용한다.
- 필요한 경우에만 구조를 확장한다. 예를 들어 Kafka·RabbitMQ는 실제 외부 서비스 분리 또는 처리량·운영 요구가 생긴 뒤 도입하며, 현재는 Spring Modulith Registry를 사용한다.
- Java `record`와 Lombok `@Value`를 사용하지 않는다. DTO와 이벤트 payload는 필요한 Lombok 어노테이션을 적극 사용해 보일러플레이트를 줄인다. 생성자 로직이 없는 불변 DTO는 `private final` 필드, `@Getter`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)`, 정적 팩토리 메서드를 사용한다. 방어 복사·입력 검증 등 생성자 로직이 필요한 경우에만 명시적 생성자를 작성한다. JPA Entity는 용도에 맞는 Lombok 어노테이션으로 작성한다.
- `private` 생성자로 이미 상속이 불가능한 DTO에는 중복으로 `final class`를 붙이지 않는다. 외부에서 상속 가능한 생성자를 제공하고 상속이 불변식이나 계약을 훼손할 때만 `final class`를 사용한다.
- 테스트는 단위 테스트와 통합 테스트만 작성한다. 단순 DTO, Lombok 생성 코드, 정적 팩토리 같은 구현 세부사항은 별도로 테스트하지 않고, 도메인·비즈니스 규칙과 클라이언트에 영향을 주는 HTTP·이벤트 같은 외부 경계 계약을 검증한다.
- API 키·비밀번호 등 시크릿은 환경 변수로만 주입한다. `application-local.yaml`과 `application-prod.yaml`에는 시크릿 값을 하드코딩하지 않는다.
- Builder, Factory Method/Factory, Strategy, Adapter, Facade를 포함해 Command, Template Method, Decorator, Chain of Responsibility, Observer 등 실무에서 널리 쓰이는 디자인 패턴은 책임 분리·변경 격리·객체 생성 복잡도 해소에 실제로 도움이 될 때 사용한다. 패턴 자체를 목적으로 추가하지 않으며, 적용 이유와 대안을 코드 또는 리뷰에서 설명할 수 있어야 한다.

## 프로젝트 이벤트 아키텍처

- 이 프로젝트는 Spring Modulith 기반의 모듈형 모놀리스로 구현합니다.
- 모듈 간 도메인 이벤트는 현재 Spring Modulith Event Publication Registry로 발행·영속·재시도합니다.
- 이벤트 발행자와 소비자는 Kafka, RabbitMQ, Redis 같은 특정 메시지 브로커의 API를 직접 참조하지 않습니다.
- 이벤트 payload에 JPA Entity, 도메인 객체, lazy proxy를 직접 넣지 않습니다. 메시지큐로 전환해도 직렬화할 수 있도록 모듈의 공개 `api`에 Lombok 기반 불변 DTO 이벤트 계약을 정의하고, ID·원시값·필요한 불변 스냅샷만 담습니다.
- 외부 메시지 브로커가 필요해질 때 Spring Modulith의 이벤트 externalization 또는 별도 adapter에서 이 이벤트 계약을 전송할 수 있도록, 브로커별 구현을 모듈 내부 adapter로 한정합니다.
- 이벤트 소비자는 적어도 한 번(at-least-once) 전달과 재전송을 전제로 멱등하게 구현합니다.
- Kafka나 RabbitMQ 전환은 모듈의 공개 이벤트 계약을 유지한 채 인프라 adapter와 운영 설정만 교체할 수 있어야 합니다.

## 프로젝트 모듈 구조와 의존성

- 최상위 패키지는 기능 모듈(`member`, `prompt`, `execution`, `enrichment`, `usage`, `search`, `ranking`)로 구성한다. 전역 `controller`, `service`, `repository` 패키지는 만들지 않는다.
- 모듈 내부는 `internal/web`, `internal/application`, `internal/domain`, `internal/persistence`, 필요한 경우 `internal/adapter`로 구성한다.
- 다른 모듈은 `internal`의 Entity, Repository, Service를 직접 import하지 않는다. `ApplicationModules.verify()`와 `@ApplicationModule(allowedDependencies = ...)`로 위반을 차단한다.
- 모듈 간 후속 처리에는 이벤트를 우선 사용한다. 화면 표시·검색처럼 최신성이 즉시 필요하지 않은 데이터는 이벤트 기반 읽기 모델을 사용한다.
- 현재 상태가 반드시 필요한 동기 검증·조회에만 공개 Facade를 사용한다. 모듈 간 REST 호출은 하지 않는다.
- 공개 Facade는 `<module>/api/facade`에 인터페이스와 DTO를 둔다. 구현체는 `<module>/internal/application`에 두며, `api` 패키지는 `@NamedInterface("api")`로 명시한다.
- 외부 시스템(OpenAI, Redis, Google OAuth2)처럼 교체·실패 격리가 필요한 경계에만 Port/Adapter를 만든다. 모듈 내부 CRUD나 Spring Data Repository에 불필요한 인터페이스 계층을 만들지 않는다.
- `common`에는 기술 공통 요소만 둔다. 도메인 Entity, 도메인 Service, 비즈니스 규칙을 넣지 않는다.

## Git 작업 규칙

- 모든 작업은 GitHub Issue와 연결한다.
- 브랜치 형식은 `<type>/#{issue-number}-{short-description}`을 사용한다. `short-description`은 소문자 kebab-case 영어로 작성한다.
- type은 `feat`, `fix`, `bug`, `refactor`, `infra`, `docs`만 사용한다.
- 커밋 첫 줄은 `<type>(#{issue-number}): <작업 목적>` 형식을 사용한다.
- 커밋 본문에는 선택 이유와 검증 결과를 기록하고, Lore Commit Protocol의 trailer 규칙을 함께 따른다.
- Pull Request 본문에는 `Closes #<issue-number>`를 사용해 관련 Issue를 연결한다.

## 1. 코딩 전에 생각하기

**추측하지 않습니다. 혼란을 숨기지 않습니다. 트레이드오프를 드러냅니다.**

구현 전:

- 가정을 명시적으로 밝힙니다. 확실하지 않으면 질문합니다.
- 여러 해석이 가능하면 제시합니다. 조용히 하나를 선택하지 않습니다.
- 더 단순한 접근이 있으면 알립니다. 필요하면 이견을 제시합니다.
- 불분명한 것이 있으면 멈춥니다. 무엇이 혼란스러운지 밝히고 질문합니다.

## 2. 단순함 우선

**문제를 해결하는 최소한의 코드만 작성합니다. 추측성 구현은 하지 않습니다.**

- 요청 범위를 벗어난 기능은 추가하지 않습니다.
- 한 번만 쓰는 코드에 추상화를 만들지 않습니다.
- 요청되지 않은 "유연성"이나 "설정 가능성"을 추가하지 않습니다.
- 발생할 수 없는 상황을 위한 예외 처리는 만들지 않습니다.
- 200줄을 썼는데 50줄로 가능하다면 다시 작성합니다.

스스로에게 묻습니다. "시니어 엔지니어가 이것을 과도하게 복잡하다고 할까?" 그렇다면 단순화합니다.

## 3. 필요한 부분만 정확히 변경

**반드시 필요한 부분만 건드립니다. 자신이 만든 문제만 정리합니다.**

기존 코드를 수정할 때:

- 인접한 코드, 주석, 서식을 "개선"하지 않습니다.
- 문제가 없는 부분을 리팩터링하지 않습니다.
- 다르게 작성하고 싶더라도 기존 스타일을 따릅니다.
- 관련 없는 죽은 코드를 발견하면 알리되 삭제하지 않습니다.

변경으로 사용되지 않는 코드가 생겼다면:

- 자신의 변경으로 미사용 상태가 된 import, 변수, 함수를 제거합니다.
- 요청받지 않았다면 기존의 죽은 코드는 제거하지 않습니다.

검증 기준: 변경한 모든 줄은 사용자의 요청과 직접 연결되어야 합니다.

## 4. 목표 중심 실행

**성공 기준을 정의하고, 검증될 때까지 반복합니다.**

작업을 검증 가능한 목표로 바꿉니다:

- "유효성 검증 추가" → "잘못된 입력을 위한 테스트를 작성하고 통과시킨다"
- "버그 수정" → "버그를 재현하는 테스트를 작성하고 통과시킨다"
- "X 리팩터링" → "변경 전후로 테스트가 통과하는지 확인한다"

여러 단계의 작업은 간단한 계획을 제시합니다:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

명확한 성공 기준이 있으면 독립적으로 반복할 수 있습니다. "작동하게 해줘" 같은 약한 기준은 지속적인 확인을 요구합니다.

---

**이 지침이 잘 작동하고 있다는 신호:** diff에서 불필요한 변경이 줄고, 과도한 복잡성으로 인한 재작성도 줄며, 실수 후가 아니라 구현 전에 명확화 질문이 나옵니다.
