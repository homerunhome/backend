# GitHub Actions CI/CD

이 저장소는 backend 단독 저장소입니다. 워크플로는 저장소 루트의
`.github/workflows/ci.yml`에 있으며, 별도 `working-directory: backend`는 사용하지 않습니다.

## 실행 시점과 검증

- 브랜치에 push하거나 main 대상 PR을 생성·갱신하면 실행합니다.
- 기본 브랜치에 반영된 뒤에는 Actions → Backend CI/CD → Run workflow로 수동 실행할 수 있습니다.
- Ubuntu에서 Temurin Java 21을 설치하고 Gradle Wrapper를 검증합니다.
- `./gradlew clean build --no-daemon`으로 전체 테스트와 실행 JAR 빌드를 수행합니다.
- 테스트는 `src/test/resources/application.yaml`의 H2를 사용합니다.
  CI에는 운영 DB 비밀번호나 실제 외부 API 키를 등록할 필요가 없습니다.
- 배포 스크립트 문법과 정상 배포·실패 복구를 테스트합니다.
- 동일 브랜치 또는 PR에 새로운 실행이 생기면 이전 실행을 취소합니다.
  main 실행은 배포 도중 취소하지 않고 순차 처리합니다.
- 테스트 보고서와 성공한 빌드의 실행 JAR은 Actions 실행 화면에서 3일 동안 다운로드할 수 있습니다.
  `*-plain.jar`는 배포용 산출물에서 제외합니다.

PR 병합 조건을 설정할 경우 `Test and build (Java 21)` 검사를 필수로 지정합니다.
워크플로 추가만으로 저장소의 브랜치 보호 설정이 바뀌지는 않습니다.

## AWS 배포와 상시 실행

main push 또는 main 수동 실행에서 빌드 성공 후 EC2에 JAR을 SSH/SCP로 배포합니다.
PR과 다른 브랜치는 테스트·빌드만 실행합니다. Docker와 ECR은 사용하지 않습니다.
서버 준비와 GitHub Secrets 등록은 [배포 설정](deployment.md)을 따릅니다.
설정 전에는 deploy 작업이 실패하며, 워크플로 파일만 추가했다고 서버 연결이 완료되지는 않습니다.

서버의 systemd가 프로세스 장애 및 서버 재부팅 후 앱을 재시작합니다.
배포 실패 시 이전 JAR로 복구를 시도하며, 최초 배포 실패 시 서비스를 중지합니다.

상시 실행은 AWS 서버가 실행 중인 동안 유지됩니다.
서버 중지나 크레딧 소진을 GitHub Actions가 대신 해결하지는 않습니다.
