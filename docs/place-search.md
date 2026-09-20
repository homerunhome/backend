# 장소 검색

키워드, 카카오 카테고리, 지도 영역을 조합하여 일정에 추가할 장소 정보를 검색합니다.

## API

```http
POST /api/places/search
Content-Type: application/json

{
  "keyword": "성심당",
  "category": "FOOD",
  "bounds": {
    "southWestLatitude": 36.30,
    "southWestLongitude": 127.38,
    "northEastLatitude": 36.38,
    "northEastLongitude": 127.48
  },
  "page": 1,
  "size": 15
}
```

키워드 검색에서는 `category`와 `bounds`를 생략할 수 있습니다. 키워드 없이 카테고리만 검색할 때는 카카오 카테고리 검색 API의 요구사항에 따라 `bounds`가 필수입니다.

## 응답 예시

```json
{
  "places": [
    {
      "id": "123456",
      "name": "성심당 본점",
      "address": "대전 중구 은행동 145",
      "roadAddress": "대전 중구 대종로480번길 15",
      "latitude": 36.327681,
      "longitude": 127.427314,
      "category": "FOOD",
      "categoryName": "음식점 > 제과,베이커리",
      "phone": "1588-8069",
      "placeUrl": "https://place.map.kakao.com/123456",
      "distanceMeters": null
    }
  ],
  "page": 1,
  "size": 15,
  "totalCount": 1,
  "pageableCount": 1,
  "hasNext": false
}
```

검색 결과가 없으면 오류 대신 `200 OK`, 빈 `places`, `hasNext: false`를 반환합니다. 인증 실패, 호출 한도 초과, 카카오 서버 장애는 각각 #4에서 정의한 카카오 API 오류 코드로 반환합니다.

## 카테고리

| 요청 값 | 카카오 코드 | 의미 |
| --- | --- | --- |
| `FOOD` | `FD6` | 음식점 |
| `CAFE` | `CE7` | 카페 |
| `TOURIST_ATTRACTION` | `AT4` | 관광명소 |
| `CULTURAL_FACILITY` | `CT1` | 문화시설 |
| `ACCOMMODATION` | `AD5` | 숙박 |
| `PARKING` | `PK6` | 주차장 |
| `SUBWAY_STATION` | `SW8` | 지하철역 |

그 밖의 카카오 공식 카테고리도 `PlaceCategory`에 정의되어 있습니다. 응답의 카카오 카테고리 코드가 알려지지 않은 값이면 `OTHER`로 반환합니다. `OTHER`는 검색 조건으로 사용할 수 없습니다.

## 페이지 처리

- `page`: 1~45, 기본값 1
- `size`: 1~15, 기본값 15
- `totalCount`: 카카오가 검색한 전체 문서 수
- `pageableCount`: 카카오가 노출할 수 있는 문서 수
- `hasNext`: 다음 페이지 요청 가능 여부

## 실제 검색 테스트

실제 카카오 검색 테스트는 `KAKAO_REST_API_KEY`가 설정된 경우에만 실행됩니다.

```bash
KAKAO_REST_API_KEY=발급받은_REST_API_키 \
  ./gradlew test --tests '*PlaceSearchLiveIntegrationTest'
```

카카오 앱에서 카카오맵·로컬 API(`OPEN_MAP_AND_LOCAL`) 사용 설정이 활성화되어 있어야 합니다. 호출 허용 IP를 설정한 경우 테스트 실행 서버의 공인 출발 IP도 등록해야 합니다.
