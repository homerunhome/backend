# HomerunHome 백엔드 하네스

| 구성 | 위치 | 역할 |
| --- | --- | --- |
| Rule | `AGENTS.md` | 개발 원칙과 작업 범위 관리 |
| Hook | `.githooks/` | 커밋 메시지, 민감정보, 충돌 마커, 테스트 확인 |
| Skill | `.agents/skills/` | 기능 구현, 리뷰, CI 실패 분석 절차 |
| Template | `.github/`, `.codex/` | 이슈·PR·커밋 형식 통일 |

## 검증 수준

- 커밋 메시지, 보호 브랜치, 민감정보·충돌 마커·대용량 파일은 로컬 Hook에서 확인합니다.
- Java, Gradle 설정 또는 테스트 변경이 포함된 커밋은 `./gradlew test`를 실행합니다.
- PR 전에는 `./gradlew clean build`로 빌드를 확인합니다.
- 원격 브랜치 보호와 강제 푸시 제한은 GitHub 저장소 설정에서 관리합니다.

## 시작

```bash
./scripts/install-hooks.sh
```

기능 구현은 `backend-feature`, PR 전 자체 점검은 `code-review`, CI 장애 분석은 `ci-failure-analysis` 절차를 따릅니다.
