# 탐색 위치·장소 종류별 장소 조회

코스 편집 화면에서 검색할 위치를 먼저 선택하고 음식점·카페·편의점·관광명소·문화시설 등의 장소 종류를 선택해 후보 장소를 조회합니다. 이 API는 장소 후보만 제공하며 일정 변경, 체류시간 저장, 시간 재계산은 일정·코스 기능에서 처리합니다.

## API

```http
POST /api/places/explore
Content-Type: application/json
```

야구장 주변 음식점 조회 예시는 다음과 같습니다.

```json
{
  "scope": "STADIUM_NEARBY",
  "category": "FOOD",
  "page": 1,
  "size": 15
}
```

키워드를 추가하면 선택한 위치와 장소 종류 안에서 결과를 좁힙니다.

```json
{
  "scope": "STATION_NEARBY",
  "category": "CAFE",
  "keyword": "디저트",
  "page": 1,
  "size": 15
}
```

## 검색 위치

| `scope` | 기준 | 서버 적용 범위 | 추가 입력 |
| --- | --- | ---: | --- |
| `COURSE_NEARBY` | 편집할 기존 장소 또는 앞뒤 장소 사이 구간 | 중심 반경 1km 또는 전달받은 지도 영역 | `center` 또는 `bounds` |
| `STADIUM_NEARBY` | 대전 한화생명 볼파크 | 반경 1.5km | 없음 |
| `STATION_NEARBY` | 대전역 | 반경 1km | 없음 |
| `CURRENT_LOCATION` | 사용자 기기의 현재 위치 | 반경 1km | `center` |
| `DAEJEON_HOTSPOT` | 선택한 대전 주요 지역 | 지역별 1~1.5km | `hotspot` |
| `CUSTOM` | 지도에서 선택한 영역 | 전달받은 지도 영역 | `bounds` |

기존 장소를 바꿀 때는 해당 장소 좌표를 `COURSE_NEARBY.center`로 전달합니다. 두 장소 사이에 새 장소를 추가할 때는 앞뒤 장소를 포함하도록 계산한 지도 영역을 `COURSE_NEARBY.bounds`로 전달합니다.

```json
{
  "scope": "COURSE_NEARBY",
  "category": "TOURIST_ATTRACTION",
  "bounds": {
    "southWestLatitude": 36.31,
    "southWestLongitude": 127.42,
    "northEastLatitude": 36.34,
    "northEastLongitude": 127.45
  }
}
```

현재 위치는 WGS84 위도·경도를 사용합니다.

```json
{
  "scope": "CURRENT_LOCATION",
  "category": "CONVENIENCE_STORE",
  "center": {
    "latitude": 36.3504,
    "longitude": 127.3845
  }
}
```

## 대전 주요 지역

| `hotspot` | 지역 | 반경 |
| --- | --- | ---: |
| `EUNHAENG_DAEHEUNG` | 은행동·대흥동 | 1.2km |
| `SOJE_DONG` | 소제동 | 1km |
| `DUNSAN_DONG` | 둔산동 | 1.5km |
| `EXPO` | 엑스포·한밭수목원 주변 | 1.5km |
| `YUSEONG` | 유성온천 주변 | 1.5km |

```json
{
  "scope": "DAEJEON_HOTSPOT",
  "hotspot": "SOJE_DONG",
  "category": "CAFE"
}
```

## 장소 종류

초기 편집 화면에서 우선 노출할 장소 종류는 다음과 같습니다.

| 사용자 표시 | `category` | 카카오 분류 |
| --- | --- | --- |
| 먹을 곳 | `FOOD` | 음식점 `FD6` |
| 카페 | `CAFE` | 카페 `CE7` |
| 편의점 | `CONVENIENCE_STORE` | 편의점 `CS2` |
| 놀거리·관광 | `TOURIST_ATTRACTION` | 관광명소 `AT4` |
| 문화시설 | `CULTURAL_FACILITY` | 문화시설 `CT1` |

카카오에는 `놀거리` 단일 분류가 없으므로 관광명소와 문화시설을 구분합니다. 숙박·주차장·병원처럼 `PlaceCategory`에 정의된 다른 카테고리도 같은 API로 요청할 수 있습니다. `OTHER`는 검색 조건으로 사용할 수 없습니다.

## 응답

```json
{
  "scope": "STADIUM_NEARBY",
  "hotspot": null,
  "places": [
    {
      "id": "place-1",
      "name": "야구장 근처 식당",
      "address": "대전 중구 부사동 1",
      "roadAddress": "대전 중구 대종로 1",
      "latitude": 36.32,
      "longitude": 127.43,
      "category": "FOOD",
      "categoryName": "음식점 > 한식",
      "phone": "042-123-4567",
      "placeUrl": "https://place.map.kakao.com/place-1",
      "distanceMeters": 320
    }
  ],
  "page": 1,
  "size": 15,
  "totalCount": 1,
  "pageableCount": 1,
  "hasNext": false
}
```

조회 결과가 없으면 `200 OK`, 빈 `places`, `hasNext: false`를 반환합니다. 잘못된 범위 조합은 `INVALID_PLACE_EXPLORATION_REQUEST`, 인증·한도·카카오 서버 오류는 기존 카카오 공통 오류로 반환합니다.

## 실제 API 검증

`KAKAO_REST_API_KEY`를 설정하면 대전 한화생명 볼파크 반경 1.5km의 실제 음식점을 검증하는 테스트가 실행됩니다.

```bash
KAKAO_REST_API_KEY=발급받은_REST_API_키 ./gradlew test \
  --tests '*PlaceExploreLiveIntegrationTest'
```

카카오 앱에서 카카오맵·로컬 API(`OPEN_MAP_AND_LOCAL`) 사용 설정이 활성화되어 있어야 합니다.
