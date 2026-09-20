# TourAPI 기반 대전 관광정보

한국관광공사 TourAPI를 사용해 대전 관광 콘텐츠, 상세 소개와 사진, 경기 날짜 전후 행사, 무장애 편의정보를 제공합니다. 카카오 API는 일반 장소와 위치 탐색에 사용하고, TourAPI는 공식 관광 콘텐츠 정보를 제공하는 역할로 구분합니다.

## 활용 API

| 활용신청 서비스 | 오퍼레이션 | 용도 |
| --- | --- | --- |
| 국문 관광정보 서비스 | `areaBasedList2` | 대전 관광 콘텐츠와 `contentId` 조회 |
| 국문 관광정보 서비스 | `locationBasedList2` | 코스 기준 위치 주변 관광 콘텐츠 조회 |
| 국문 관광정보 서비스 | `detailCommon2` | 관광 콘텐츠 상세 소개 조회 |
| 국문 관광정보 서비스 | `detailImage2` | 대표·추가 이미지 조회 |
| 국문 관광정보 서비스 | `searchFestival2` | 날짜별 대전 행사 조회 |
| 무장애 여행 정보 | `detailWithTour2` | 장애인·어르신·영유아 동반 편의정보 조회 |

모든 지역 조회에는 대전 지역코드 `3`을 적용합니다.

## 환경변수

```env
TOUR_API_SERVICE_KEY='공공데이터포털_디코딩_인증키'
```

선택 설정은 다음과 같습니다.

```env
TOUR_API_BASE_URL=https://apis.data.go.kr/B551011
TOUR_API_MOBILE_APP=HomerunHome
TOUR_API_CONNECT_TIMEOUT=2s
TOUR_API_READ_TIMEOUT=5s
```

로컬에서는 Git에 포함되지 않는 `.env`, EC2에서는 `~/homerunhome/app.env`에 저장합니다. 인증키, 전체 요청 URL과 외부 API 응답 본문은 로그에 남기지 않습니다.

## 대전 관광 콘텐츠 목록

```http
GET /api/tourism/contents?type=TOURIST_ATTRACTION&page=1&size=12
```

지원하는 `type`은 다음과 같습니다.

| 값 | TourAPI 콘텐츠 유형 |
| --- | --- |
| `TOURIST_ATTRACTION` | 관광지 `12` |
| `CULTURAL_FACILITY` | 문화시설 `14` |
| `FESTIVAL_EVENT` | 축제·공연·행사 `15` |
| `TRAVEL_COURSE` | 여행코스 `25` |
| `LEISURE_SPORTS` | 레포츠 `28` |

```json
{
  "contentType": "TOURIST_ATTRACTION",
  "contents": [
    {
      "contentId": "125407",
      "contentType": "TOURIST_ATTRACTION",
      "title": "한밭수목원",
      "address": "대전 서구 둔산대로 169",
      "addressDetail": "",
      "latitude": 36.366,
      "longitude": 127.388,
      "phone": "042-123-4567",
      "imageUrl": "https://example.com/image.jpg",
      "thumbnailUrl": "https://example.com/thumbnail.jpg"
    }
  ],
  "page": 1,
  "size": 12,
  "totalCount": 1,
  "hasNext": false
}
```

## 관광 콘텐츠 상세·사진·무장애 정보

```http
GET /api/tourism/contents/{contentId}
```

프론트엔드가 관광지를 선택하면 목록 응답의 `contentId`를 전달합니다. 백엔드가 `detailCommon2`, `detailImage2`, `detailWithTour2` 결과를 하나로 합칩니다.

```json
{
  "contentId": "125407",
  "contentType": "TOURIST_ATTRACTION",
  "title": "한밭수목원",
  "overview": "도심 속 수목원입니다.",
  "address": "대전 서구 둔산대로 169",
  "addressDetail": "",
  "latitude": 36.366,
  "longitude": 127.388,
  "phone": "042-123-4567",
  "homepageUrl": "https://example.com",
  "imageUrl": "https://example.com/image.jpg",
  "thumbnailUrl": "https://example.com/thumbnail.jpg",
  "images": [
    {
      "name": "한밭수목원 전경",
      "originalUrl": "https://example.com/original.jpg",
      "thumbnailUrl": "https://example.com/small.jpg",
      "copyrightType": "Type1"
    }
  ],
  "accessibility": {
    "parking": "장애인 주차구역 있음",
    "wheelchair": "휠체어 대여 가능",
    "restroom": "장애인 화장실 있음",
    "stroller": "유모차 대여 가능",
    "lactationRoom": "수유실 있음"
  }
}
```

추가 이미지가 없으면 `images`는 빈 배열입니다. TourAPI에 무장애 정보가 없으면 `accessibility`는 `null`입니다. 소개에 포함된 HTML은 일반 텍스트로 변환해 반환합니다.

## 경기 날짜 전후 대전 행사

```http
GET /api/tourism/events?startDate=2026-09-20&endDate=2026-09-21&page=1&size=12
```

조회 시작일과 종료일은 필수이며 최대 31일 범위까지 허용합니다.

```json
{
  "startDate": "2026-09-20",
  "endDate": "2026-09-21",
  "events": [
    {
      "contentId": "event-1",
      "title": "대전 축제",
      "startDate": "2026-09-20",
      "endDate": "2026-09-21",
      "address": "대전 중구",
      "addressDetail": "",
      "latitude": 36.33,
      "longitude": 127.43,
      "phone": "042-123-4567",
      "imageUrl": "https://example.com/event.jpg",
      "thumbnailUrl": "https://example.com/event-thumbnail.jpg"
    }
  ],
  "page": 1,
  "size": 12,
  "totalCount": 1,
  "hasNext": false
}
```

## 오류 처리

| 상황 | HTTP 상태 | 오류 코드 |
| --- | ---: | --- |
| 인증키 미설정 | 503 | `TOUR_API_KEY_NOT_CONFIGURED` |
| 인증·활용 권한 실패 | 502 | `TOUR_API_AUTHENTICATION_FAILED` |
| 호출 한도 초과 | 429 | `TOUR_API_QUOTA_EXCEEDED` |
| TourAPI 요청 파라미터 거부 | 502 | `TOUR_API_INVALID_REQUEST` |
| TourAPI 서버·통신 오류 | 502 | `TOUR_API_EXTERNAL_SERVER_ERROR` |
| TourAPI 응답 형식 오류 | 502 | `TOUR_API_RESPONSE_INVALID` |
| 서비스 요청 조건 오류 | 400 | `INVALID_TOURISM_REQUEST` |
| 존재하지 않는 관광 콘텐츠 | 404 | `TOURISM_CONTENT_NOT_FOUND` |

조회 결과가 없으면 목록 API는 `200 OK`와 빈 배열을 반환합니다.

## 실제 API 검증

```bash
TOUR_API_SERVICE_KEY='디코딩_인증키' ./gradlew test \
  --tests '*TourismLiveIntegrationTest'
```

환경변수가 없으면 실제 API 테스트만 건너뜁니다.
