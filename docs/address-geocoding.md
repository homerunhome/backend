# 주소 좌표 변환

사용자가 입력한 도로명 또는 지번 주소를 카카오 주소 검색으로 확인하고 지도·경로 조회에 사용할 위도와 경도로 변환합니다.

## API

```http
POST /api/geocoding
Content-Type: application/json

{
  "address": "대전 중구 대종로 373"
}
```

집 주소가 URL과 접근 로그에 노출되지 않도록 요청 본문으로 전달합니다.

성공 응답은 위도와 경도를 이름으로 명확히 구분합니다.

```json
{
  "addressName": "대전 중구 부사동 209-1",
  "roadAddressName": "대전 중구 대종로 373",
  "latitude": 36.317067,
  "longitude": 127.429188
}
```

## 검색 결과 처리 기준

1. 입력 주소의 앞뒤 공백을 제거하고 연속된 공백을 하나로 합칩니다.
2. 빈 주소는 `ADDRESS_REQUIRED`로 거절합니다.
3. 200자를 초과하거나 제어문자를 포함한 주소는 `INVALID_ADDRESS`로 거절합니다.
4. 검색 결과가 없으면 `ADDRESS_NOT_FOUND`를 반환합니다.
5. 결과가 하나면 해당 결과를 반환합니다.
6. 결과가 여러 개면 입력값과 도로명 또는 지번 주소가 정확히 일치하는 결과를 선택합니다.
7. 정확히 일치하는 결과가 없거나 여러 개면 `ADDRESS_AMBIGUOUS`를 반환합니다.

## 테스트

일반 테스트는 카카오 클라이언트를 모킹하므로 API 키 없이 실행할 수 있습니다.

```bash
./gradlew test
```

실제 주소 변환 테스트는 `KAKAO_REST_API_KEY`가 설정된 경우에만 실행됩니다.

```bash
KAKAO_REST_API_KEY=발급받은_REST_API_키 \
  ./gradlew test --tests '*AddressGeocodingLiveIntegrationTest'
```

403 응답에 `disabled OPEN_MAP_AND_LOCAL service`가 포함되면 카카오디벨로퍼스에서 해당 앱의 카카오맵·로컬 API 사용 설정을 활성화해야 합니다. 호출 허용 IP를 설정한 키라면 테스트를 실행하는 서버의 공인 출발 IP도 등록해야 합니다.
