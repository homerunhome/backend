# 경기 전 코스 생성

`POST /api/itineraries/generate`는 선택한 경기, 도착·귀가 교통편, 장소별 체류시간과 구간별 이동시간으로 코스를 시간순 계산해 저장합니다. 장소 후보는 [`/api/course-place-candidates`](course-place-candidates.md)에서 조회할 수 있으며, 관광지·문화시설·행사 후보에는 TourAPI 결과가 포함됩니다.

## 요청

```http
POST /api/itineraries/generate
Content-Type: application/json
```

```json
{
  "gameId": "game-2026-09-20-001",
  "arrivalPlace": "대전역",
  "arrivalAt": "2026-09-20T12:00:00",
  "arrivalTrain": {
    "trainNumber": "001",
    "trainType": "KTX",
    "departureStation": "서울역",
    "departureAt": "2026-09-20T11:00:00",
    "arrivalStation": "대전역",
    "arrivalAt": "2026-09-20T12:00:00"
  },
  "departurePlace": "대전역",
  "departureAt": "2026-09-20T23:30:00",
  "returnTrain": {
    "trainNumber": "122",
    "trainType": "KTX",
    "departureStation": "대전역",
    "departureAt": "2026-09-20T23:30:00",
    "arrivalStation": "서울역",
    "arrivalAt": "2026-09-21T00:30:00"
  },
  "stadiumEntryBufferMinutes": 30,
  "finalLegToStadiumMinutes": 20,
  "stadiumToDepartureMinutes": 45,
  "postGameCrowdBufferMinutes": 30,
  "boardingBufferMinutes": 15,
  "expectedGameDurationMinutes": 180,
  "preferences": ["FOOD", "CAFE"],
  "places": [
    {
      "placeId": "tour-api-content-id",
      "name": "한밭수목원",
      "address": "대전광역시 서구 둔산대로 169",
      "latitude": 36.366,
      "longitude": 127.388,
      "travelFromPreviousMinutes": 20,
      "stayDurationMinutes": 60,
      "mustVisit": true
    }
  ]
}
```

`places`는 방문 순서대로 전달합니다. 첫 장소의 `travelFromPreviousMinutes`는 도착 교통편 장소에서 첫 장소까지의 시간이며, `finalLegToStadiumMinutes`는 마지막 장소에서 경기장까지의 시간입니다. 장소가 없으면 마지막 구간 시간은 도착 장소에서 경기장까지로 사용합니다. 시간 단위는 분입니다.

`preferences`는 선택한 관광 성향 목록이며 생성된 일정에 저장되어 응답과 이후 조회에 포함됩니다. 장소 후보를 고르고 방문 순서를 정하는 기능은 수행하지 않으므로, 요청의 `places`에는 이미 선택한 후보를 전달합니다.

`arrivalTrain`과 `returnTrain`에는 선택한 왕편·귀가편 시간표를 전달합니다. 두 값은 선택 사항이며, 전달한 경우 열차 번호·종류·역·출발·도착 시각을 일정에 저장하고 응답 및 상세 조회에 포함합니다.

`expectedGameDurationMinutes`는 예상 경기 시간이며 생략하면 180분을 사용합니다. 응답의 `expectedGameEndAt`은 실제 종료 확정 시각이 아니며, `gameEndMayBeDelayed`와 경고를 통해 연장전 등으로 늦어질 수 있음을 알립니다.

## 실제 이동시간 기반 자동 순서 생성

`POST /api/itineraries/generate/optimized`는 이미 선택한 장소 후보 최대 8곳을 모두 방문하도록 구간별 실제 이동시간 합계가 가장 짧은 순서를 계산한 뒤 일정을 저장합니다. 장소 검색·추천은 하지 않습니다. 호출자는 공통 [`POST /api/travel-times`](travel-time.md) API로 구한 이동시간을 `travelTimes`에 전달합니다.

`travelTimes`에는 방향별 구간이 빠짐없이 있어야 합니다. `ARRIVAL`에서 각 후보까지, 각 후보에서 다른 후보까지, 각 후보에서 `STADIUM`까지의 시간입니다. 후보가 없으면 `ARRIVAL`에서 `STADIUM`까지 한 구간만 보냅니다. 분 단위 정수로 보내며, 공통 API가 초 단위로 반환한 시간은 분으로 올림해 전달합니다. 대중교통처럼 방향에 따라 시간이 다를 수 있으므로 역방향 구간은 별도로 조회합니다.

```json
{
  "gameId": "game-2026-09-20-001",
  "arrivalPlace": "대전역",
  "arrivalAt": "2026-09-20T12:00:00",
  "departurePlace": "대전역",
  "departureAt": "2026-09-20T23:30:00",
  "stadiumEntryBufferMinutes": 30,
  "stadiumToDepartureMinutes": 45,
  "postGameCrowdBufferMinutes": 30,
  "boardingBufferMinutes": 15,
  "travelMode": "PUBLIC_TRANSIT",
  "places": [
    {"placeId": "place-a", "name": "장소 A", "stayDurationMinutes": 60},
    {"placeId": "place-b", "name": "장소 B", "stayDurationMinutes": 45}
  ],
  "travelTimes": [
    {"fromPlaceId": "ARRIVAL", "toPlaceId": "place-a", "minutes": 20},
    {"fromPlaceId": "ARRIVAL", "toPlaceId": "place-b", "minutes": 15},
    {"fromPlaceId": "place-a", "toPlaceId": "place-b", "minutes": 12},
    {"fromPlaceId": "place-b", "toPlaceId": "place-a", "minutes": 18},
    {"fromPlaceId": "place-a", "toPlaceId": "STADIUM", "minutes": 25},
    {"fromPlaceId": "place-b", "toPlaceId": "STADIUM", "minutes": 10}
  ]
}
```

응답의 `places`는 계산된 방문 순서이며, 각 장소의 `travelFromPreviousMinutes`와 경기장까지 마지막 이동시간으로 도착·체류 시각을 계산합니다. 이동시간 구간이 누락되거나 중복되고, 후보 ID가 중복되거나 8곳을 넘으면 `INVALID_ITINERARY_PLAN_REQUEST`를 반환합니다. 이동시간이 같은 순서가 여러 개면 입력 후보 순서를 우선합니다.

## 저장된 일정 장소 편집

`PUT /api/itineraries/{id}/places`는 저장된 일정의 장소 전체 목록을 요청 배열 순서로 교체합니다. 원하는 최종 목록을 보내면 장소 추가·삭제·교체·재정렬과 체류시간 변경이 한 번에 적용되고, 편집 후 시간표가 다시 계산됩니다. `places: []`를 보내면 방문 장소를 모두 삭제합니다.

장소 편집은 저장된 왕편·귀가편 열차 선택을 유지합니다.

장소는 [`/api/course-place-candidates`](course-place-candidates.md)의 후보 정보를 사용하고, 각 구간 이동시간은 공통 [`POST /api/travel-times`](travel-time.md) API의 결과를 분 단위로 전달합니다. 이 편집 API는 Kakao를 직접 호출하지 않습니다. 공통 이동시간 API는 자동차·도보·대중교통 경로를 지원합니다. 첫 장소의 `travelFromPreviousMinutes`는 도착 장소에서 이동하는 시간이고, 나머지는 앞 장소에서 이동하는 시간입니다. 마지막 장소에서 경기장까지는 `finalLegToStadiumMinutes`로 전달합니다.

```http
PUT /api/itineraries/1/places
Content-Type: application/json
```

```json
{
  "stadiumEntryBufferMinutes": 30,
  "finalLegToStadiumMinutes": 20,
  "stadiumToDepartureMinutes": 45,
  "postGameCrowdBufferMinutes": 30,
  "boardingBufferMinutes": 15,
  "expectedGameDurationMinutes": 180,
  "places": [
    {
      "placeId": "tour-api-content-id",
      "name": "한밭수목원",
      "address": "대전광역시 서구 둔산대로 169",
      "latitude": 36.366,
      "longitude": 127.388,
      "travelFromPreviousMinutes": 20,
      "stayDurationMinutes": 60,
      "mustVisit": true
    }
  ]
}
```

응답은 계산된 장소별 도착·출발 시각과 이동시간, 경기장 도착 예정시각, 경기 시작까지 여유, 경기장 출발 권장시각, `returnTransportBoardable` 탑승 가능 여부, 상태와 경고를 반환합니다. 시간이 부족하거나 귀가편을 탈 수 없는 입력도 저장을 차단하지 않고 원인 경고와 함께 계산 결과를 반환합니다. 일정 ID가 없으면 `ITINERARY_NOT_FOUND`, 필드 값이 유효하지 않으면 `VALIDATION_ERROR`를 반환합니다.

## 응답 계산

- `stadiumArrivalAt`: 장소 체류·이동시간과 마지막 경기장 이동시간의 누적값
- `gameStartSlackMinutes`: 경기 시작 시각과 경기장 도착 예정 시각 사이의 분
- `stadiumDepartureRecommendedAt`: 귀가 교통편 출발에서 역 이동·경기 후 혼잡·탑승 여유시간을 역산한 경기장 출발 권장시각
- `returnTransportBoardable`: 예상 경기 종료시각까지 기준으로 귀가 교통편 탑승 가능 여부
- `status`: `AMPLE`, `ADEQUATE`, `TIGHT`, `GAME_LATE_POSSIBLE`, `RETURN_CAUTION`
- `warnings`: 예상 종료 불확실성, 경기장 입장 여유 부족, 귀가편 위험 안내

입장 여유시간을 뺀 시간이 60분 이상이면 `AMPLE`, 15분 이상 60분 미만이면 `ADEQUATE`, 0분 이상 15분 미만이면 `TIGHT`, 0분 미만이면 `GAME_LATE_POSSIBLE`입니다. 예상 종료시각부터 권장 경기장 출발까지 60분 미만이면 `RETURN_CAUTION`이 우선하며, 다른 경고도 함께 반환합니다.

상태 표시는 각각 여유 있음, 적당함, 조금 촉박함, 경기 지각 가능, 귀가 주의를 뜻합니다.

시간이 부족해도 입력된 장소와 계산된 일정을 모두 반환합니다. 시간 초과 여부를 확인해 코스를 조정할 수 있도록 상태와 경고를 제공합니다.
