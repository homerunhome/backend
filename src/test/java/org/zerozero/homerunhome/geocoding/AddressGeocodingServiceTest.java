package org.zerozero.homerunhome.geocoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.geocoding.service.AddressGeocodingService;
import org.zerozero.homerunhome.kakao.client.KakaoApiClient;
import org.zerozero.homerunhome.kakao.dto.common.KakaoCoordinate;
import org.zerozero.homerunhome.kakao.dto.common.KakaoSearchResult;
import org.zerozero.homerunhome.kakao.dto.request.KakaoAddressSearchRequest;
import org.zerozero.homerunhome.kakao.dto.response.KakaoAddress;

class AddressGeocodingServiceTest {

    private KakaoApiClient kakaoApiClient;
    private AddressGeocodingService service;

    @BeforeEach
    void setUp() {
        kakaoApiClient = mock(KakaoApiClient.class);
        service = new AddressGeocodingService(kakaoApiClient);
    }

    @Test
    void returnsLatitudeAndLongitudeForOneAddress() {
        KakaoAddressSearchRequest request = new KakaoAddressSearchRequest("대전 중구 대종로 373", 1, 30);
        when(kakaoApiClient.searchAddresses(request))
                .thenReturn(result(1, address(
                        "대전 중구 부사동 209-1",
                        "대전 중구 대종로 373",
                        "36.317067",
                        "127.429188")));

        var response = service.geocode("  대전  중구 대종로 373  ");

        assertEquals(new BigDecimal("36.317067"), response.latitude());
        assertEquals(new BigDecimal("127.429188"), response.longitude());
        assertEquals("대전 중구 대종로 373", response.roadAddressName());
        verify(kakaoApiClient).searchAddresses(request);
    }

    @Test
    void selectsTheOnlyExactMatchFromMultipleResults() {
        KakaoAddress exact = address("서울 광진구 구의동 546-4", "서울 광진구 광나루로56길 85", "37.5385", "127.0824");
        KakaoAddress similar = address("서울 광진구 구의동 546-5", "서울 광진구 광나루로56길 86", "37.5386", "127.0825");
        when(kakaoApiClient.searchAddresses(new KakaoAddressSearchRequest("서울 광진구 광나루로56길 85", 1, 30)))
                .thenReturn(result(2, similar, exact));

        var response = service.geocode("서울 광진구 광나루로56길 85");

        assertEquals("서울 광진구 광나루로56길 85", response.roadAddressName());
        assertEquals(new BigDecimal("37.5385"), response.latitude());
    }

    @Test
    void rejectsAddressWhenThereIsNoResult() {
        when(kakaoApiClient.searchAddresses(new KakaoAddressSearchRequest("없는 주소", 1, 30)))
                .thenReturn(new KakaoSearchResult<>(List.of(), 0, 0, true));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.geocode("없는 주소"));

        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void rejectsMultipleResultsWithoutOneExactMatch() {
        when(kakaoApiClient.searchAddresses(new KakaoAddressSearchRequest("광나루로", 1, 30)))
                .thenReturn(result(
                        2,
                        address("서울 광진구 구의동 1", "서울 광진구 광나루로 1", "37.5", "127.1"),
                        address("서울 광진구 구의동 2", "서울 광진구 광나루로 2", "37.6", "127.2")));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.geocode("광나루로"));

        assertEquals(ErrorCode.ADDRESS_AMBIGUOUS, exception.getErrorCode());
    }

    @Test
    void rejectsBlankAddressBeforeCallingKakao() {
        BusinessException exception = assertThrows(BusinessException.class, () -> service.geocode("  "));

        assertEquals(ErrorCode.ADDRESS_REQUIRED, exception.getErrorCode());
        verify(kakaoApiClient, never()).searchAddresses(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsAddressContainingControlCharacters() {
        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.geocode("서울 광진구\n광나루로"));

        assertEquals(ErrorCode.INVALID_ADDRESS, exception.getErrorCode());
        verify(kakaoApiClient, never()).searchAddresses(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsAddressLongerThanTwoHundredCharacters() {
        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.geocode("가".repeat(201)));

        assertEquals(ErrorCode.INVALID_ADDRESS, exception.getErrorCode());
        verify(kakaoApiClient, never()).searchAddresses(org.mockito.ArgumentMatchers.any());
    }

    private KakaoSearchResult<KakaoAddress> result(int totalCount, KakaoAddress... addresses) {
        return new KakaoSearchResult<>(List.of(addresses), totalCount, totalCount, true);
    }

    private KakaoAddress address(
            String addressName, String roadAddressName, String latitude, String longitude) {
        return new KakaoAddress(
                addressName,
                roadAddressName,
                new KakaoCoordinate(new BigDecimal(longitude), new BigDecimal(latitude)));
    }
}
