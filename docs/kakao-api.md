# 카카오 API 공통 클라이언트

`KakaoApiClient`는 일정·코스 기능에서 카카오 원본 응답 형식에 의존하지 않도록 주소 검색, 장소 검색, 자동차 이동시간 조회를 공통 형식으로 제공합니다.

## 패키지 구조

```text
kakao
├── client
│   ├── KakaoApiClient.java       # 다른 기능이 사용하는 공통 진입점
│   ├── local                     # 주소·장소 검색과 카카오 Local 응답
│   ├── mobility                  # 자동차 경로 조회와 Mobility 응답
│   └── support                   # 인증 확인과 공통 오류 변환
├── config                        # RestClient와 타임아웃 설정
└── dto
    ├── common                    # 좌표·검색 목록처럼 공통인 값
    ├── request                   # 공통 클라이언트 입력값
    └── response                  # 일정·코스 기능에 반환하는 값
```

일정·코스 코드는 `client.KakaoApiClient`와 `dto`만 사용합니다. `local`, `mobility`, `support`는 카카오 연동 내부 구현입니다.

## 환경변수

필수 값은 카카오디벨로퍼스에서 발급한 **REST API 키**입니다. JavaScript 키나 Admin 키를 넣지 않습니다.

```text
KAKAO_REST_API_KEY=발급받은_REST_API_키
```

다음 값은 기본값이 있어 필요한 경우에만 설정합니다.

```text
KAKAO_LOCAL_BASE_URL=https://dapi.kakao.com
KAKAO_MOBILITY_BASE_URL=https://apis-navi.kakaomobility.com
KAKAO_CONNECT_TIMEOUT=2s
KAKAO_READ_TIMEOUT=3s
```

로컬에서는 Git에 포함되지 않는 `.env`에 값을 보관하고 IDE 실행 설정 또는 터미널 환경변수로 주입합니다. EC2에서는 `~/homerunhome/app.env`에 추가한 뒤 서비스를 재시작합니다.

```bash
sudo systemctl restart homerunhome.service
sudo journalctl -u homerunhome.service -n 100 --no-pager
```

카카오디벨로퍼스에서 REST API 키의 **호출 허용 IP 주소**를 제한했다면 실제 API를 호출하는 EC2의 공인 출발 IP를 등록해야 합니다. EC2 공인 IP가 바뀌는 구성이라면 Elastic IP 사용을 권장합니다.

## A 담당자가 사용하는 계약

공통 진입점은 `org.zerozero.homerunhome.kakao.client.KakaoApiClient`입니다.

| 기능 | 메서드 | 입력 | 반환 |
| --- | --- | --- | --- |
| 주소 → 좌표 | `searchAddresses` | `KakaoAddressSearchRequest` | `KakaoSearchResult<KakaoAddress>` |
| 키워드 장소 검색 | `searchPlaces` | `KakaoPlaceSearchRequest` | `KakaoSearchResult<KakaoPlace>` |
| 이동 거리·시간 | `findDrivingRoute` | `KakaoRouteRequest` | `KakaoRoute` |

기본 검색 예시는 다음과 같습니다.

```java
KakaoSearchResult<KakaoAddress> addresses = kakaoApiClient.searchAddresses(
        KakaoAddressSearchRequest.firstPage("대전 한화생명 볼파크"));

KakaoSearchResult<KakaoPlace> places = kakaoApiClient.searchPlaces(
        KakaoPlaceSearchRequest.firstPage("성심당"));
```

위치 반경과 거리순 검색이 필요하면 중심 좌표를 함께 전달합니다.

```java
KakaoPlaceSearchRequest request = new KakaoPlaceSearchRequest(
        "카페",
        new KakaoCoordinate(new BigDecimal("127.429188"), new BigDecimal("36.317067")),
        3000,
        KakaoPlaceSearchSort.DISTANCE,
        1,
        15);
```

`KakaoRoute`는 카카오모빌리티 자동차 경로 정보입니다. 공통 [`/api/travel-times`](travel-time.md) API는 이 경로 외에도 카카오맵 REST API로 도보와 대중교통의 거리·소요시간을 조회합니다. 대중교통 결과는 경로 소요시간이며 열차 시간표나 막차 조회는 TAGO 열차 API에서 별도로 처리합니다.

## 오류 구분

| 상황 | 오류 코드 |
| --- | --- |
| 키 미설정 | `KAKAO_API_KEY_NOT_CONFIGURED` |
| 인증·권한 실패(401, 403) | `KAKAO_AUTHENTICATION_FAILED` |
| 호출 한도 초과(429) | `KAKAO_QUOTA_EXCEEDED` |
| 카카오 서버 오류(5xx) | `KAKAO_EXTERNAL_SERVER_ERROR` |
| 자동차 경로 탐색 결과 없음 | `ROUTE_NOT_FOUND` |
| 그 밖의 잘못된 응답·통신 오류 | `KAKAO_API_REQUEST_FAILED` |

로그에는 작업 이름과 HTTP 상태만 남기며 REST API 키와 카카오 응답 본문은 기록하지 않습니다.

## 공식 문서

- [카카오 Local REST API](https://developers.kakao.com/docs/ko/local/dev-guide)
- [카카오 모빌리티 자동차 길찾기 API](https://developers.kakaomobility.com/guide/navi-api/directions)
- [카카오 REST API 오류 코드](https://developers.kakao.com/docs/ko/rest-api/reference)
