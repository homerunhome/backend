# 경기 일정 등록 및 조회

경기 일정을 직접 등록하고 목록 또는 식별자로 조회합니다. 외부 경기 일정 자동 동기화는 제공하지 않습니다.

## 경기 등록

```http
POST /api/games
Content-Type: application/json
```

```json
{
  "gameId": "game-2026-09-22-001",
  "city": "대전",
  "gameDate": "2026-09-22",
  "gameStartTime": "18:30:00",
  "stadium": "대전 한화생명 볼파크",
  "homeTeam": "한화 이글스",
  "awayTeam": "롯데 자이언츠"
}
```

등록 성공은 `201 Created`를 반환합니다. 이미 등록된 `gameId`는 `409 GAME_ALREADY_EXISTS`로 거부합니다.

## 경기 조회

- `GET /api/games`: 전체 경기 목록을 날짜와 시작 시각순으로 조회합니다.
- `GET /api/games/{gameId}`: 식별자로 경기 한 건을 조회합니다. 없으면 `404 GAME_NOT_FOUND`를 반환합니다.
