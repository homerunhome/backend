package org.zerozero.homerunhome.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.zerozero.homerunhome.kakao.client.KakaoApiClient;
import org.zerozero.homerunhome.kakao.dto.common.KakaoCoordinate;
import org.zerozero.homerunhome.kakao.dto.common.KakaoMapBounds;
import org.zerozero.homerunhome.kakao.dto.common.KakaoSearchResult;
import org.zerozero.homerunhome.kakao.dto.request.KakaoCategoryPlaceSearchRequest;
import org.zerozero.homerunhome.kakao.dto.request.KakaoPlaceSearchRequest;
import org.zerozero.homerunhome.kakao.dto.request.KakaoPlaceSearchSort;
import org.zerozero.homerunhome.kakao.dto.response.KakaoPlace;
import org.zerozero.homerunhome.place.dto.MapBounds;
import org.zerozero.homerunhome.place.dto.PlaceCategory;
import org.zerozero.homerunhome.place.dto.PlaceSearchRequest;
import org.zerozero.homerunhome.place.service.PlaceSearchService;

class PlaceSearchServiceTest {

    private KakaoApiClient kakaoApiClient;
    private PlaceSearchService service;

    @BeforeEach
    void setUp() {
        kakaoApiClient = mock(KakaoApiClient.class);
        service = new PlaceSearchService(kakaoApiClient);
    }

    @Test
    void searchesByKeywordCategoryAndMapBounds() {
        KakaoMapBounds kakaoBounds = kakaoBounds();
        KakaoPlaceSearchRequest kakaoRequest = new KakaoPlaceSearchRequest(
                "성심당", null, null, "FD6", kakaoBounds, KakaoPlaceSearchSort.ACCURACY, 1, 10);
        when(kakaoApiClient.searchPlaces(kakaoRequest))
                .thenReturn(new KakaoSearchResult<>(List.of(place()), 24, 24, false));

        var response = service.search(new PlaceSearchRequest(
                "  성심당  ", PlaceCategory.FOOD, bounds(), 1, 10));

        assertThat(response.places()).hasSize(1);
        assertEquals("place-1", response.places().getFirst().id());
        assertEquals(PlaceCategory.FOOD, response.places().getFirst().category());
        assertEquals(new BigDecimal("36.327681"), response.places().getFirst().latitude());
        assertEquals(new BigDecimal("127.427314"), response.places().getFirst().longitude());
        assertEquals(1, response.page());
        assertEquals(10, response.size());
        assertEquals(24, response.totalCount());
        assertThat(response.hasNext()).isTrue();
        verify(kakaoApiClient).searchPlaces(kakaoRequest);
    }

    @Test
    void searchesByCategoryWhenKeywordIsMissing() {
        KakaoCategoryPlaceSearchRequest kakaoRequest =
                new KakaoCategoryPlaceSearchRequest("CE7", kakaoBounds(), 1, 15);
        when(kakaoApiClient.searchPlacesByCategory(kakaoRequest))
                .thenReturn(new KakaoSearchResult<>(List.of(), 0, 0, true));

        var response = service.search(new PlaceSearchRequest(null, PlaceCategory.CAFE, bounds(), null, null));

        assertThat(response.places()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        assertEquals(1, response.page());
        assertEquals(15, response.size());
        verify(kakaoApiClient).searchPlacesByCategory(kakaoRequest);
    }

    @Test
    void returnsAnEmptyPageWhenThereAreNoKeywordResults() {
        when(kakaoApiClient.searchPlaces(any(KakaoPlaceSearchRequest.class)))
                .thenReturn(new KakaoSearchResult<>(List.of(), 0, 0, true));

        var response = service.search(new PlaceSearchRequest("없는 장소", null, null, 1, 15));

        assertThat(response.places()).isEmpty();
        assertEquals(0, response.totalCount());
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void rejectsRequestWithoutKeywordAndCategory() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.search(new PlaceSearchRequest(null, null, bounds(), 1, 15)));

        assertEquals(ErrorCode.INVALID_PLACE_SEARCH_REQUEST, exception.getErrorCode());
        verify(kakaoApiClient, never()).searchPlaces(any());
        verify(kakaoApiClient, never()).searchPlacesByCategory(any());
    }

    @Test
    void rejectsCategoryOnlySearchWithoutMapBounds() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.search(new PlaceSearchRequest(null, PlaceCategory.FOOD, null, 1, 15)));

        assertEquals(ErrorCode.INVALID_PLACE_SEARCH_REQUEST, exception.getErrorCode());
    }

    @Test
    void rejectsReversedMapBounds() {
        MapBounds reversed = new MapBounds(
                new BigDecimal("36.38"),
                new BigDecimal("127.48"),
                new BigDecimal("36.30"),
                new BigDecimal("127.38"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.search(new PlaceSearchRequest("카페", null, reversed, 1, 15)));

        assertEquals(ErrorCode.INVALID_PLACE_SEARCH_REQUEST, exception.getErrorCode());
    }

    @Test
    void keepsExternalApiFailureDistinctFromEmptyResults() {
        when(kakaoApiClient.searchPlaces(any(KakaoPlaceSearchRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.KAKAO_EXTERNAL_SERVER_ERROR));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.search(new PlaceSearchRequest("카페", null, null, 1, 15)));

        assertEquals(ErrorCode.KAKAO_EXTERNAL_SERVER_ERROR, exception.getErrorCode());
    }

    private MapBounds bounds() {
        return new MapBounds(
                new BigDecimal("36.30"),
                new BigDecimal("127.38"),
                new BigDecimal("36.38"),
                new BigDecimal("127.48"));
    }

    private KakaoMapBounds kakaoBounds() {
        return new KakaoMapBounds(
                new KakaoCoordinate(new BigDecimal("127.38"), new BigDecimal("36.30")),
                new KakaoCoordinate(new BigDecimal("127.48"), new BigDecimal("36.38")));
    }

    private KakaoPlace place() {
        return new KakaoPlace(
                "place-1",
                "성심당 본점",
                "음식점 > 제과,베이커리",
                "FD6",
                "1588-8069",
                "대전 중구 은행동 145",
                "대전 중구 대종로480번길 15",
                new KakaoCoordinate(new BigDecimal("127.427314"), new BigDecimal("36.327681")),
                "https://place.map.kakao.com/place-1",
                null);
    }
}
