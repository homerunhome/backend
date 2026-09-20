# 카카오·TourAPI 통합 코스 장소 후보 조회

자동 코스 추천과 수동 코스 편집에서 사용할 장소 후보를 위치 범위와 카테고리 기준으로 조회합니다. 카카오 장소와 TourAPI 관광 콘텐츠를 하나의 형식으로 반환하므로 일정·코스 기능은 외부 제공처별 응답을 직접 처리하지 않아도 됩니다.

전체 코스 생성, 방문 장소 수와 순서 결정, 장소별 체류시간, 코스 저장·수정과 일정 재계산은 일정·코스 기능에서 처리합니다.

## API

```http
POST /api/course-place-candidates
Content-Type: application/json
```

대전 야구장 주변의 음식점, 카페, 관광지를 조회하는 요청입니다.

```json
{
  "scope": "STADIUM_NEARBY",
  "categories": ["FOOD", "CAFE", "TOURIST_ATTRACTION"],
  "maxDistanceMeters": 2000,
  "limit": 30
}
```

경기일에 열리는 행사까지 조회하려면 `eventDate`를 전달합니다.

```json
{
  "scope": "STATION_NEARBY",
  "categories": ["CAFE", "CULTURAL_FACILITY", "FESTIVAL_EVENT"],
  "keyword": "전시",
  "maxDistanceMeters": 3000,
  "eventDate": "2026-09-20",
  "limit": 20
}
```

## 요청 기준

| 필드 | 필수 | 기준 |
| --- | --- | --- |
| `scope` | 예 | 검색 위치 범위 |
| `categories` | 예 | 한 개 이상의 장소 카테고리 |
| `center` | 조건부 | 현재 위치·기존 코스·직접 지정 범위에서 필수 |
| `coursePoints` | 조건부 | `COURSE_NEARBY`에서 기존 코스 좌표를 최대 10개까지 전달 |
| `hotspot` | 조건부 | `DAEJEON_HOTSPOT`에서 필수 |
| `keyword` | 아니요 | 장소명 검색어, 최대 100자 |
| `maxDistanceMeters` | 아니요 | 1~20,000m |
| `eventDate` | 조건부 | `FESTIVAL_EVENT` 선택 시 필수 |
| `limit` | 아니요 | 1~50개, 기본값 30개 |

지원하는 `scope`은 다음과 같습니다.

| 값 | 중심 위치 | 기본 반경 |
| --- | --- | ---: |
| `STADIUM_NEARBY` | 대전 한화생명 볼파크 | 1,500m |
| `STATION_NEARBY` | 대전역 | 1,000m |
| `CURRENT_LOCATION` | `center` | 1,000m |
| `COURSE_NEARBY` | 기존 코스의 `coursePoints`, 없으면 `center` | 각 좌표 1,000m |
| `CUSTOM` | 사용자가 지정한 `center` | 1,000m |
| `DAEJEON_HOTSPOT` | 선택한 `hotspot` | 명소별 기본 반경 |

`DAEJEON_HOTSPOT`에서는 `EUNHAENG_DAEHEUNG`, `SOJE_DONG`, `DUNSAN_DONG`, `EXPO`, `YUSEONG`을 선택할 수 있습니다.

지원하는 `categories`와 조회 제공처는 다음과 같습니다.

| 카테고리 | 제공처 |
| --- | --- |
| `FOOD` | 카카오 |
| `CAFE` | 카카오 |
| `CONVENIENCE_STORE` | 카카오 |
| `ACTIVITY` | 카카오 키워드 검색 |
| `TOURIST_ATTRACTION` | 카카오 + TourAPI |
| `CULTURAL_FACILITY` | 카카오 + TourAPI |
| `FESTIVAL_EVENT` | TourAPI 경기일 행사 검색 |

TourAPI 관광지와 문화시설은 `locationBasedList2`를 이용해 중심 좌표 주변을 조회합니다. 행사는 `searchFestival2`로 날짜에 맞는 대전 행사를 조회한 뒤 중심 좌표와의 거리를 적용합니다.

카테고리는 여러 개를 함께 선택할 수 있습니다. 결과는 모든 카테고리와 검색 중심점의 후보를 합치고 중복을 제거한 뒤 거리순으로 반환합니다. 카카오와 TourAPI는 저장 수·리뷰 수를 제공하지 않으므로 인기순 정렬은 제공하지 않습니다. 카카오 결과가 한 페이지 15개를 넘으면 `limit`을 채우는 데 필요한 다음 페이지까지 조회합니다.

## 기존 코스 전체 주변 요청 예시

```json
{
  "scope": "COURSE_NEARBY",
  "categories": ["FOOD", "CAFE", "TOURIST_ATTRACTION"],
  "coursePoints": [
    {"latitude": 36.3322, "longitude": 127.4345},
    {"latitude": 36.3277, "longitude": 127.4273},
    {"latitude": 36.317067, "longitude": 127.429188}
  ],
  "maxDistanceMeters": 1000,
  "limit": 30
}
```

## 현재 위치 요청 예시

```json
{
  "scope": "CURRENT_LOCATION",
  "categories": ["FOOD", "CAFE", "ACTIVITY"],
  "center": {
    "latitude": 36.3504,
    "longitude": 127.3845
  },
  "searchCenters": [
    {"latitude": 36.317067, "longitude": 127.429188}
  ],
  "maxDistanceMeters": 1500,
  "limit": 20
}
```

## 응답 예시

```json
{
  "scope": "STADIUM_NEARBY",
  "center": {
    "latitude": 36.317067,
    "longitude": 127.429188
  },
  "maxDistanceMeters": 2000,
  "places": [
    {
      "provider": "KAKAO",
      "externalId": "123456789",
      "name": "추천 맛집",
      "category": "FOOD",
      "address": "대전 중구 대종로",
      "phone": "042-123-4567",
      "latitude": 36.318,
      "longitude": 127.43,
      "imageUrl": null,
      "detailUrl": "https://place.map.kakao.com/123456789",
      "eventStartDate": null,
      "eventEndDate": null,
      "distanceMeters": 130
    },
    {
      "provider": "TOUR_API",
      "externalId": "125407",
      "name": "한밭수목원",
      "category": "TOURIST_ATTRACTION",
      "address": "대전 서구 둔산대로 169",
      "phone": "042-123-4567",
      "latitude": 36.366,
      "longitude": 127.388,
      "imageUrl": "https://example.com/image.jpg",
      "detailUrl": "/api/tourism/contents/125407",
      "eventStartDate": null,
      "eventEndDate": null,
      "distanceMeters": 850
    }
  ]
}
```

## 필터와 중복 처리

1. 모든 결과의 중심 좌표 직선거리를 미터 단위로 다시 계산합니다.
2. `maxDistanceMeters`를 벗어난 결과를 제거합니다.
3. 장소명의 공백·기호를 제거한 값이 같고 좌표가 100m 이내인 결과를 동일 장소로 판단합니다.
4. 중복 결과 중 이미지가 있는 결과를 우선하고, 조건이 같으면 중심 위치에 가까운 결과를 유지합니다.
5. 최종 결과를 거리순으로 정렬한 뒤 `limit`만큼 반환합니다.

조건에 맞는 장소가 없으면 `200 OK`와 빈 `places`를 반환합니다. 잘못된 조건은 `INVALID_COURSE_PLACE_CANDIDATE_REQUEST`로 반환하며, 카카오와 TourAPI 인증·호출 한도·외부 서버 오류는 각 공통 오류 코드를 유지합니다.

## 실제 API 검증

```bash
KAKAO_REST_API_KEY='카카오_REST_API_키' \
TOUR_API_SERVICE_KEY='TourAPI_디코딩_인증키' \
./gradlew test --tests '*CoursePlaceCandidateLiveIntegrationTest'
```

두 환경변수 중 하나라도 없으면 실제 API 테스트만 건너뜁니다.
