# AI 프롬프트 저장소

## 로컬 실행

`.env.example`을 복사해 로컬 시크릿을 설정한 뒤 MySQL을 실행한다. 애플리케이션도 동일한 `.env` 파일을 설정으로 불러와 Compose와 같은 자격 증명을 사용한다.

```bash
cp .env.example .env
docker compose -f docker-compose.local.yml up -d
./gradlew bootRun
```

MySQL 볼륨을 생성한 뒤 `.env`의 `MYSQL_PASSWORD`를 바꿨다면 컨테이너 환경 변수만 바뀌고 기존 MySQL 계정 비밀번호는 바뀌지 않는다. 기존 비밀번호를 `.env`에 유지하거나, 로컬 데이터 삭제가 가능할 때만 Compose 볼륨을 재생성한다.

## 개발 워크플로우

개인 프로젝트이지만 작업 목적, 변경 범위, 검증 결과를 GitHub Issue와 Pull Request에 남긴다.
모든 기능·수정 작업은 Issue로 시작하고, 해당 Issue 번호를 브랜치와 커밋 메시지에 포함한다.

### 작업 유형

| 유형 | 용도 |
| --- | --- |
| `feat` | 새로운 사용자 기능 |
| `fix` | 확인된 기능 결함 수정 |
| `bug` | 버그 재현, 원인 분석, 회귀 테스트 보강 |
| `refactor` | 동작을 변경하지 않는 구조 개선 |
| `infra` | Docker, CI/CD, 배포, 환경 설정 |
| `docs` | README, 설계 문서, 운영 문서 |

### 브랜치 이름

형식은 다음과 같다.

```text
<type>/#{issue-number}-{short-description}
```

예시:

```text
feat/#12-create-prompt
fix/#27-public-prompt-permission
infra/#8-add-local-compose
docs/#4-document-module-architecture
```

`short-description`은 소문자 kebab-case 영어로 작성한다. 브랜치는 하나의 Issue만 해결하며, 작업이 끝나면 Pull Request를 통해 병합한다.

### 커밋 메시지

형식은 다음과 같다.

```text
<type>(#{issue-number}): <summary>
```

예시:

```text
feat(#12): 프롬프트 생성 API 추가
fix(#27): 비공개 프롬프트 접근 권한 수정
refactor(#31): 프롬프트 생성 유스케이스 분리
infra(#8): 로컬 MySQL Compose 추가
docs(#4): 모듈 아키텍처 가이드 작성
```

커밋 summary는 현재형으로 간결하게 작성하고, 하나의 커밋에는 하나의 논리적 변경만 포함한다.

### Issue와 Pull Request

- Issue에는 문제 또는 목표, 완료 조건, 검증 방법을 작성한다.
- Pull Request에는 해결한 Issue, 주요 변경, 테스트 결과, 설계상 트레이드오프를 작성한다.
- PR 본문에 `Closes #<issue-number>`를 사용해 병합 시 Issue가 자동으로 닫히게 한다.
