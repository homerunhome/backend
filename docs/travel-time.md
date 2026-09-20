# 장소 간 이동시간 조회 API

출발지와 도착지의 WGS84 좌표를 받아 이동시간과 거리를 반환합니다. 현재 일반 공개 API로 사용할 수 있는 카카오모빌리티 자동차 길찾기만 지원합니다.

## 지원 이동수단

| 요청 값 | 지원 여부 | 기준 |
| --- | --- | --- |
| `CAR` | 지원 | 카카오모빌리티 자동차 길찾기 API |
| `WALK` | 미지원 | 카카오모빌리티 제휴 계약 필요 |
| `BICYCLE` | 미지원 | 카카오모빌리티 제휴 계약 필요 |
| `PUBLIC_TRANSIT` | 미지원 | 카카오모빌리티 제휴 계약 필요 |

미지원 이동수단은 임의 계산값을 반환하지 않고 `UNSUPPORTED_TRAVEL_MODE` 오류로 처리합니다.

## API 호출 방법

```http
POST /api/travel-times
Content-Type: application/json
```

잠실야구장에서 서울역까지 자동차 이동시간을 조회하는 예시는 다음과 같습니다.

```json
{
  "origin": {
    "latitude": 37.5121,
    "longitude": 127.0719
  },
  "destination": {
    "latitude": 37.5547,
    "longitude": 126.9707
  },
  "transportMode": "CAR"
}
```

응답 예시는 다음과 같습니다. 교통 상황에 따라 값은 달라질 수 있습니다.

```json
{
  "transportMode": "CAR",
  "durationSeconds": 1280,
  "distanceMeters": 15100
}
```

- `durationSeconds`: 예상 이동시간, 단위는 초
- `distanceMeters`: 예상 이동거리, 단위는 미터

## 오류 처리

| 상황 | HTTP 상태 | 오류 코드 |
| --- | --- | --- |
| 좌표 누락 또는 WGS84 범위 오류 | 400 | `INVALID_TRAVEL_TIME_REQUEST` |
| 도보·자전거·대중교통 요청 | 400 | `UNSUPPORTED_TRAVEL_MODE` |
| 경로를 찾을 수 없음 | 404 | `ROUTE_NOT_FOUND` |
| 카카오 인증·권한 실패 | 502 | `KAKAO_AUTHENTICATION_FAILED` |
| 카카오 호출 한도 초과 | 429 | `KAKAO_QUOTA_EXCEEDED` |
| 카카오 서버 오류 | 502 | `KAKAO_EXTERNAL_SERVER_ERROR` |

## 실제 API 검증

`KAKAO_REST_API_KEY`를 설정하면 잠실야구장과 서울역 사이의 실제 자동차 경로를 검증하는 테스트가 실행됩니다.

```bash
KAKAO_REST_API_KEY=발급받은_REST_API_키 ./gradlew test \
  --tests '*TravelTimeLiveIntegrationTest'
```

카카오디벨로퍼스에서 해당 REST API 키의 길찾기 API 사용 권한과 호출 허용 IP를 먼저 설정해야 합니다. API 키는 저장소나 테스트 코드에 기록하지 않습니다.

## 공식 문서

- [카카오모빌리티 자동차 길찾기 API](https://developers.kakaomobility.com/guide/navi-api/directions)
- [카카오모빌리티 길찾기 결과 코드](https://developers.kakaomobility.com/guide/navi-api/reference.html)
- [카카오모빌리티 제휴 API](https://developers.kakaomobility.com/affiliate/)
