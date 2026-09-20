# GitHub Actions CI

이 저장소는 backend 단독 저장소입니다. 워크플로는 저장소 루트의
`.github/workflows/ci.yml`에 있으며, 별도 `working-directory: backend`는 사용하지 않습니다.

## 실행 시점과 검증

- 브랜치에 push하거나 main 대상 PR을 생성·갱신하면 실행합니다.
- 기본 브랜치에 반영된 뒤에는 Actions → Backend CI → Run workflow로 수동 실행할 수 있습니다.
- Ubuntu에서 Temurin Java 21을 설치하고 Gradle Wrapper를 검증합니다.
- `./gradlew clean build --no-daemon`으로 전체 테스트와 실행 JAR 빌드를 수행합니다.
- 테스트는 `src/test/resources/application.yaml`의 H2를 사용합니다.
  CI에는 운영 DB 비밀번호나 실제 외부 API 키를 등록할 필요가 없습니다.
- 동일 브랜치 또는 PR에 새로운 실행이 생기면 이전 실행을 취소합니다.
- 테스트 보고서와 성공한 빌드의 실행 JAR은 Actions 실행 화면에서 3일 동안 다운로드할 수 있습니다.
  `*-plain.jar`는 배포용 산출물에서 제외합니다.

PR 병합 조건을 설정할 경우 `Test and build (Java 21)` 검사를 필수로 지정합니다.
워크플로 추가만으로 저장소의 브랜치 보호 설정이 바뀌지는 않습니다.

## AWS 배포와 상시 실행

현재 이 워크플로는 테스트·빌드만 수행하며 AWS에 배포하지 않습니다.
GitHub Actions의 실행이 종료된 뒤에도 서비스가 동작하려면 AWS 서버에 앱을 배포하고
Docker 재시작 정책 또는 systemd 서비스를 설정해야 합니다.

CD 연결 전 확인할 정보:

- 서버 종류(EC2/Lightsail), 리전, OS, CPU 아키텍처
- 서버 접속 방법과 배포 권한
- MySQL 위치, DB 이름과 계정, 데이터 보존 및 백업 방식
- 서비스 공개 주소와 배포 후 정상 응답 확인 방법

비밀번호·개인키는 저장소나 문서에 기록하지 않습니다.
서버에서 필요한 기존 환경변수는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
`JPA_DDL_AUTO`, `TAGO_SERVICE_KEY`입니다.
`TAGO_SERVICE_KEY`는 열차 API 사용 시 필요합니다.

상시 실행은 AWS 서버가 실행 중인 동안 유지됩니다.
서버 중지나 크레딧 소진을 GitHub Actions가 대신 해결하지는 않습니다.
