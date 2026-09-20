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
  "departurePlace": "대전역",
  "departureAt": "2026-09-20T23:30:00",
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

`places`는 방문 순서대로 전달합니다. 첫 장소의 `travelFromPreviousMinutes`는 도착 교통편 장소에서 첫 장소까지의 시간이며, `finalLegToStadiumMinutes`는 마지막 장소에서 경기장까지의 시간입니다. 장소가 없으면 마지막 구간 시간은 도착 장소에서 경기장까지로 사용합니다. 시간 단위는 분입니다.

`expectedGameDurationMinutes`는 예상 경기 시간이며 생략하면 180분을 사용합니다. 응답의 `expectedGameEndAt`은 실제 종료 확정 시각이 아니며, `gameEndMayBeDelayed`와 경고를 통해 연장전 등으로 늦어질 수 있음을 알립니다.

## 응답 계산

- `stadiumArrivalAt`: 장소 체류·이동시간과 마지막 경기장 이동시간의 누적값
- `gameStartSlackMinutes`: 경기 시작 시각과 경기장 도착 예정 시각 사이의 분
- `stadiumDepartureRecommendedAt`: 귀가 교통편 출발에서 역 이동·경기 후 혼잡·탑승 여유시간을 역산한 경기장 출발 권장시각
- `status`: `AMPLE`, `ADEQUATE`, `TIGHT`, `GAME_LATE_POSSIBLE`, `RETURN_CAUTION`
- `warnings`: 예상 종료 불확실성, 경기장 입장 여유 부족, 귀가편 위험 안내

입장 여유시간을 뺀 시간이 60분 이상이면 `AMPLE`, 15분 이상 60분 미만이면 `ADEQUATE`, 0분 이상 15분 미만이면 `TIGHT`, 0분 미만이면 `GAME_LATE_POSSIBLE`입니다. 예상 종료시각부터 권장 경기장 출발까지 60분 미만이면 `RETURN_CAUTION`이 우선하며, 다른 경고도 함께 반환합니다.

상태 표시는 각각 여유 있음, 적당함, 조금 촉박함, 경기 지각 가능, 귀가 주의를 뜻합니다.

시간이 부족해도 입력된 장소와 계산된 일정을 모두 반환합니다. 시간 초과 여부를 확인해 코스를 조정할 수 있도록 상태와 경고를 제공합니다.
