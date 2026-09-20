# 사용자 선호·코스 조건 기반 장소 추천

일정·코스 기능에서 전달한 선호 카테고리와 위치 조건을 사용해 실제 장소 후보를 반환합니다. 이 API는 카카오 장소 조회, 기준점 거리 계산, 최대 거리 필터, 중복 제거와 거리순 정렬을 담당합니다. 사용자 선호 저장, 코스 생성·편집, 체류시간 저장과 일정 재계산은 일정·코스 기능에서 처리합니다.

## API

```http
POST /api/places/recommendations
Content-Type: application/json
```

대전 한화생명 볼파크 주변의 음식점과 카페를 추천하는 요청은 다음과 같습니다.

```json
{
  "categories": ["FOOD", "CAFE"],
  "keyword": "디저트",
  "referencePoint": {
    "latitude": 36.317067,
    "longitude": 127.429188
  },
  "maxDistanceMeters": 1500,
  "limit": 10
}
```

## 요청 필드

| 필드 | 필수 | 기준 |
| --- | --- | --- |
| `categories` | 예 | 선호 카테고리 1~5개, 중복 값은 한 번만 적용 |
| `keyword` | 아니요 | 공백이면 적용하지 않으며 최대 100자 |
| `referencePoint` | 예 | 거리 계산 기준 WGS84 위도·경도 |
| `maxDistanceMeters` | 아니요 | 1~20,000m, 기본값 1,500m |
| `courseBounds` | 아니요 | 현재 코스에서 검색할 남서·북동 좌표 범위 |
| `limit` | 아니요 | 반환할 후보 수 1~15개, 기본값 15개 |

`referencePoint`에는 A 담당 기능에서 선택한 야구장·대전역·현재 코스 장소 등의 좌표를 전달합니다. `courseBounds`가 있으면 카카오 검색 범위를 해당 영역으로 제한한 후, `referencePoint`에서 `maxDistanceMeters` 이내인 결과만 남깁니다.

```json
{
  "categories": ["TOURIST_ATTRACTION", "CULTURAL_FACILITY"],
  "referencePoint": {
    "latitude": 36.3322,
    "longitude": 127.4345
  },
  "maxDistanceMeters": 2000,
  "courseBounds": {
    "southWestLatitude": 36.31,
    "southWestLongitude": 127.42,
    "northEastLatitude": 36.35,
    "northEastLongitude": 127.46
  },
  "limit": 10
}
```

## 처리 기준

1. 각 선호 카테고리에 같은 키워드와 위치 범위를 적용합니다.
2. 응답 좌표와 `referencePoint` 사이의 직선거리를 미터 단위로 계산합니다.
3. `maxDistanceMeters`를 초과한 장소를 제외합니다.
4. 카카오 장소 식별자 `id`가 같은 결과를 하나로 합칩니다.
5. 가까운 장소부터 정렬하고 `limit`만큼 반환합니다.

카카오 Local API는 평점·가격대·혼잡도·영업 여부를 추천 조건으로 제공하지 않습니다. 현재 추천 순서는 위치와 사용자가 선택한 카테고리·키워드를 기준으로 합니다.

## 응답 예시

```json
{
  "categories": ["FOOD", "CAFE"],
  "keyword": "디저트",
  "referencePoint": {
    "latitude": 36.317067,
    "longitude": 127.429188
  },
  "maxDistanceMeters": 1500,
  "places": [
    {
      "id": "123456789",
      "name": "추천 카페",
      "address": "대전 중구 부사동",
      "roadAddress": "대전 중구 대종로",
      "latitude": 36.318,
      "longitude": 127.43,
      "category": "CAFE",
      "categoryName": "음식점 > 카페",
      "phone": "042-123-4567",
      "placeUrl": "https://place.map.kakao.com/123456789",
      "distanceMeters": 130
    }
  ]
}
```

조건에 맞는 장소가 없으면 `200 OK`와 빈 `places`를 반환합니다. 잘못된 조건은 `INVALID_PLACE_RECOMMENDATION_REQUEST`, 인증·호출 한도·카카오 서버 오류는 기존 카카오 공통 오류로 반환합니다.

## 실제 API 검증

```bash
KAKAO_REST_API_KEY=발급받은_REST_API_키 ./gradlew test \
  --tests '*PlaceRecommendationLiveIntegrationTest'
```

환경변수가 없으면 실제 API 테스트만 건너뜁니다.
