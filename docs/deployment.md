# EC2에 JAR 자동 배포

main push → 테스트·빌드 → 산출물 다운로드 → SCP로 JAR 전송 → systemd 재시작 → HTTP 확인.
기본 브랜치 반영 후 Actions → Backend CI/CD → Run workflow에서 main을 선택해 수동 실행할 수 있습니다.
Docker, ECR, AWS 액세스 키는 사용하지 않습니다. SSH 접속 정보는 필요합니다.

## 1. EC2 계정과 OS 확인

**EC2에 접속한 터미널**에서 실행합니다.

```bash
whoami
cat /etc/os-release
```

`whoami` 결과가 `SSH_USER`입니다. AWS 로그인 이메일이나 IAM 사용자 이름이 아닙니다.
Amazon Linux는 보통 `ec2-user`, Ubuntu는 보통 `ubuntu`입니다.
`sudo su` 등으로 root로 전환했다면 `exit`로 원래 SSH 로그인 계정으로 돌아옵니다.

## 2. 서버 준비 (최초 1회)

OS에 맞는 방법으로 아래 항목을 준비합니다. MySQL이 아직 없으면 이 단계를 먼저 완료합니다.

- Java 21 (`java -version`으로 확인)
- MySQL 서버 설치·기동 및 재부팅 후 자동 시작 설정
- `curl`, `flock`, `sha256sum`, `systemctl`, SSH/SFTP 서버
- SSH 계정이 비대화형 `sudo -n systemctl`로 `homerunhome.service`의
  `cat`, `restart`, `stop`, `is-active`를 실행할 권한

DB 관리자 세션에서 앱 전용 DB와 계정을 생성합니다. 아래 비밀번호는 실제 값으로 바꿉니다.
기존 계정이 있다면 새로 만들거나 비밀번호를 변경하지 말고 기존 설정을 사용합니다.

```sql
CREATE DATABASE homerunhome CHARACTER SET utf8mb4;
CREATE USER 'homerunhome'@'localhost' IDENTIFIED BY 'REPLACE_WITH_YOUR_DATABASE_PASSWORD';
GRANT ALL PRIVILEGES ON homerunhome.* TO 'homerunhome'@'localhost';
```

MySQL은 같은 EC2에서 로컬 접속만 허용하고, 보안 그룹에서 3306을 공개하지 않습니다.
MySQL 관리 도구로 `127.0.0.1` TCP 접속과 해당 계정의 DB 접근을 먼저 확인합니다.
앱은 8080을 사용하므로 기존 수동 실행 앱이 있다면 해당 프로세스를 확인하고 중지합니다.

**Mac의 backend 저장소 루트**에서 서비스 등록 스크립트만 보냅니다.
`EC2_USER`는 위에서 확인한 실제 계정으로 수정합니다. 키 경로도 실제 위치로 지정합니다.

```bash
EC2_USER=ec2-user
EC2_IP=54.234.199.79
KEY_FILE=/absolute/path/to/key-homerunhome.pem
scp -i "$KEY_FILE" scripts/install-service.sh "$EC2_USER@$EC2_IP:~/"
```

**EC2 터미널**에서 환경 파일을 직접 작성합니다. 환경 파일은 예제까지 Git 추적 대상에서 제외하며,
저장소에 추가하거나 Actions 산출물로 전송하지 않습니다. 기존 파일이 있으면 덮어쓰지 않습니다.

```bash
mkdir -p ~/homerunhome
test -f ~/homerunhome/app.env || (umask 077; touch ~/homerunhome/app.env)
chmod 600 ~/homerunhome/app.env
vi ~/homerunhome/app.env
```

각 항목을 `이름=값` 형식으로 작성합니다. `DB_URL`은 서버의 MySQL JDBC 주소,
`DB_USERNAME`과 `DB_PASSWORD`는 실제 앱 DB 계정 정보입니다.
`JPA_DDL_AUTO`는 현재 개발 환경에서 `update`로 설정합니다.
`TAGO_SERVICE_KEY`는 열차 API 호출 시 필요하고, `TOUR_API_SERVICE_KEY`는 관광 콘텐츠·행사·무장애 정보 조회 시 필요합니다. 환경 파일은 systemd 형식이며 `export`를 넣지 않습니다.
`SERVER_PORT=8080`은 배포 후 검사 주소와 같게 유지합니다.

```bash
bash ~/install-service.sh
```

SSH 계정으로 실행하며 앞에 sudo를 붙이지 않습니다. 스크립트가 필요한 명령에만 sudo를 사용합니다.
서비스를 부팅 시 자동 실행하도록 등록하고, 실제 첫 기동은 CD가 JAR을 보낸 뒤 수행합니다.

## 3. GitHub Secrets

backend 저장소 → Settings → Secrets and variables → Actions → New repository secret.

| 이름 | 값 |
| --- | --- |
| `SERVER_IP` | 현재 EC2 퍼블릭 IPv4 (현재 제공된 값: `54.234.199.79`) |
| `SSH_USER` | EC2의 `whoami` 결과 |
| `SSH_PRIVATE_KEY` | pem 파일의 BEGIN/END 줄을 포함한 내용 전체 |
| `SSH_KNOWN_HOSTS` | 신뢰하는 EC2 호스트 공개 키. 아래에서 생성 |

암호 없는 배포용 SSH 개인키를 지원합니다. 개인키와 DB 비밀번호는 채팅·Git·로그에 올리지 않습니다.
Mac에서 `pbcopy < /실제/경로/key-homerunhome.pem`으로 복사한 후 Secret 값에 붙여넣을 수 있습니다.

이미 신뢰해서 접속한 **EC2 터미널**에서 실행해 출력 한 줄을 `SSH_KNOWN_HOSTS`로 등록합니다.
IP가 달라졌다면 명령의 IP도 수정합니다.

```bash
printf '54.234.199.79 '
cut -d ' ' -f 1,2 /etc/ssh/ssh_host_ed25519_key.pub
```

결과 형식은 `54.234.199.79 ssh-ed25519 공개키문자열`입니다.
클라이언트의 pem 개인키와는 다릅니다. 호스트 키 파일이 없으면 OS의 실제 SSH 호스트 키 설정을 확인합니다.
배포는 이 키와 서버 키가 일치할 때만 연결하며 검증을 끄지 않습니다.

## 4. 네트워크

EC2 보안 그룹의 TCP 22에 **GitHub Actions 실행기의 출발지**가 허용되어야 합니다.
개인 Mac의 IP만 허용하면 Mac SSH는 되지만 Actions는 연결 시간 초과로 실패합니다.
기본 GitHub 호스팅 실행기는 출발지 IP가 고정되지 않습니다.
자동 배포를 계속 쓰려면 고정 출발지 실행기 또는 사설 네트워크 연결 등 접속 경로를 정해야 합니다.
이 저장소는 보안 그룹을 자동 수정하지 않으며, 22번 포트를 전 세계에 여는 설정도 수행하지 않습니다.
단발 테스트에서 확인한 실행기 IP를 허용해도 다음 실행까지 보장되지는 않습니다.

외부에서 API를 테스트하려면 TCP 8080에 필요한 접근 대상을 허용합니다.
배포 검사는 EC2 내부의 `127.0.0.1:8080`을 사용합니다.
EC2 퍼블릭 IP가 바뀌면 `SERVER_IP`와 `SSH_KNOWN_HOSTS`를 함께 갱신합니다.

## 5. 배포 확인과 복구

서버 준비와 Secrets 등록 후 main에 반영합니다. PR에서는 deploy가 건너뛰어지는 것이 정상입니다.

```bash
sudo systemctl status homerunhome.service --no-pager
curl --fail http://127.0.0.1:8080/api/games
sudo journalctl -u homerunhome.service -n 100 --no-pager
```

`/api/games`는 DB 조회까지 성공해야 200을 반환합니다. 빈 DB의 `[]`도 정상입니다.
로그에 비밀값이 있을 수 있으므로 원문을 공개하기 전에 확인합니다.

JAR과 체크섬은 `~/homerunhome/releases/<커밋>-<실행>-<재시도>/`에 저장합니다.
서버에서 체크섬 검증 후 `current` 링크를 교체하고 재시작합니다.
새 앱이 제한 시간 안에 정상 응답하지 않으면 이전 링크로 돌아가 재시작합니다.
복구 성공 여부와 관계없이 새 배포 실패는 Actions 실패로 표시합니다.
이전 버전이 없는 첫 배포 실패는 서비스를 중지합니다.

단일 프로세스 재시작 방식이므로 배포 중 잠깐 중단됩니다. JAR 복구는 DB 스키마나 데이터를 되돌리지 않습니다.
`JPA_DDL_AUTO=update`를 사용하므로 기존 버전과 호환되지 않는 DB 변경은 별도 마이그레이션 계획이 필요합니다.
기존 release는 자동 삭제하지 않습니다. 디스크 사용량을 보고 current 및 복구에 필요한 버전을 제외해 정리합니다.
앱 메모리 설정은 `-Xms128m -Xmx384m`이며, 같은 소형 EC2의 MySQL까지 포함한 메모리는 별도로 확인합니다.

## 참고

- [AWS의 EC2 리눅스 사용자 안내](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/managing-users.html)
- [GitHub 호스팅 실행기의 IP 주소](https://docs.github.com/en/actions/reference/runners/github-hosted-runners#ip-addresses)
- [작업 사이 빌드 산출물 전달](https://docs.github.com/en/actions/how-tos/writing-workflows/choosing-what-your-workflow-does/storing-and-sharing-data-from-a-workflow)
