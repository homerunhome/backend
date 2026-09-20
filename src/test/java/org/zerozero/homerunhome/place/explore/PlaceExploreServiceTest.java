package org.zerozero.homerunhome.place.explore;

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
import org.zerozero.homerunhome.place.explore.dto.DaejeonHotspot;
import org.zerozero.homerunhome.place.explore.dto.PlaceCoordinate;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreRequest;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreScope;
import org.zerozero.homerunhome.place.explore.service.PlaceExploreService;

class PlaceExploreServiceTest {

    private KakaoApiClient kakaoApiClient;
    private PlaceExploreService service;

    @BeforeEach
    void setUp() {
        kakaoApiClient = mock(KakaoApiClient.class);
        service = new PlaceExploreService(kakaoApiClient);
    }

    @Test
    void exploresFoodAroundStadiumUsingPresetRadius() {
        KakaoCategoryPlaceSearchRequest kakaoRequest = KakaoCategoryPlaceSearchRequest.around(
                "FD6", coordinate("127.429188", "36.317067"), 1500, 1, 15);
        when(kakaoApiClient.searchPlacesByCategory(kakaoRequest))
                .thenReturn(new KakaoSearchResult<>(List.of(place()), 1, 1, true));

        var response = service.explore(request(
                PlaceExploreScope.STADIUM_NEARBY, PlaceCategory.FOOD, null, null, null, null));

        assertEquals(PlaceExploreScope.STADIUM_NEARBY, response.scope());
        assertThat(response.places()).hasSize(1);
        assertEquals("place-1", response.places().getFirst().id());
        verify(kakaoApiClient).searchPlacesByCategory(kakaoRequest);
    }

    @Test
    void appliesKeywordAndCategoryAroundDaejeonStation() {
        KakaoPlaceSearchRequest kakaoRequest = new KakaoPlaceSearchRequest(
                "디저트",
                coordinate("127.4345", "36.3322"),
                1000,
                "CE7",
                null,
                KakaoPlaceSearchSort.DISTANCE,
                1,
                15);
        when(kakaoApiClient.searchPlaces(kakaoRequest))
                .thenReturn(new KakaoSearchResult<>(List.of(), 0, 0, true));

        var response = service.explore(request(
                PlaceExploreScope.STATION_NEARBY,
                PlaceCategory.CAFE,
                "  디저트  ",
                null,
                null,
                null));

        assertThat(response.places()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        verify(kakaoApiClient).searchPlaces(kakaoRequest);
    }

    @Test
    void exploresAroundCurrentLocation() {
        PlaceCoordinate current = new PlaceCoordinate(new BigDecimal("36.3504"), new BigDecimal("127.3845"));
        KakaoCategoryPlaceSearchRequest kakaoRequest = KakaoCategoryPlaceSearchRequest.around(
                "CS2", coordinate("127.3845", "36.3504"), 1000, 1, 15);
        when(kakaoApiClient.searchPlacesByCategory(kakaoRequest))
                .thenReturn(new KakaoSearchResult<>(List.of(), 0, 0, true));

        service.explore(request(
                PlaceExploreScope.CURRENT_LOCATION,
                PlaceCategory.CONVENIENCE_STORE,
                null,
                current,
                null,
                null));

        verify(kakaoApiClient).searchPlacesByCategory(kakaoRequest);
    }

    @Test
    void exploresInsideCourseBounds() {
        MapBounds bounds = bounds();
        KakaoMapBounds kakaoBounds = new KakaoMapBounds(
                coordinate("127.42", "36.31"), coordinate("127.45", "36.34"));
        KakaoCategoryPlaceSearchRequest kakaoRequest =
                new KakaoCategoryPlaceSearchRequest("AT4", kakaoBounds, 1, 15);
        when(kakaoApiClient.searchPlacesByCategory(kakaoRequest))
                .thenReturn(new KakaoSearchResult<>(List.of(), 0, 0, true));

        service.explore(request(
                PlaceExploreScope.COURSE_NEARBY,
                PlaceCategory.TOURIST_ATTRACTION,
                null,
                null,
                bounds,
                null));

        verify(kakaoApiClient).searchPlacesByCategory(kakaoRequest);
    }

    @Test
    void appliesSelectedDaejeonHotspot() {
        KakaoCategoryPlaceSearchRequest kakaoRequest = KakaoCategoryPlaceSearchRequest.around(
                "CT1", coordinate("127.4410", "36.3353"), 1000, 1, 15);
        when(kakaoApiClient.searchPlacesByCategory(kakaoRequest))
                .thenReturn(new KakaoSearchResult<>(List.of(), 0, 0, true));

        var response = service.explore(request(
                PlaceExploreScope.DAEJEON_HOTSPOT,
                PlaceCategory.CULTURAL_FACILITY,
                null,
                null,
                null,
                DaejeonHotspot.SOJE_DONG));

        assertEquals(DaejeonHotspot.SOJE_DONG, response.hotspot());
        verify(kakaoApiClient).searchPlacesByCategory(kakaoRequest);
    }

    @Test
    void rejectsCurrentLocationWithoutCoordinate() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.explore(request(
                        PlaceExploreScope.CURRENT_LOCATION,
                        PlaceCategory.CAFE,
                        null,
                        null,
                        null,
                        null)));

        assertEquals(ErrorCode.INVALID_PLACE_EXPLORATION_REQUEST, exception.getErrorCode());
        verify(kakaoApiClient, never()).searchPlacesByCategory(any());
    }

    @Test
    void rejectsHotspotScopeWithoutHotspot() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.explore(request(
                        PlaceExploreScope.DAEJEON_HOTSPOT,
                        PlaceCategory.FOOD,
                        null,
                        null,
                        null,
                        null)));

        assertEquals(ErrorCode.INVALID_PLACE_EXPLORATION_REQUEST, exception.getErrorCode());
    }

    @Test
    void keepsExternalApiFailureDistinctFromEmptyResults() {
        when(kakaoApiClient.searchPlacesByCategory(any()))
                .thenThrow(new BusinessException(ErrorCode.KAKAO_EXTERNAL_SERVER_ERROR));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.explore(request(
                        PlaceExploreScope.STADIUM_NEARBY,
                        PlaceCategory.FOOD,
                        null,
                        null,
                        null,
                        null)));

        assertEquals(ErrorCode.KAKAO_EXTERNAL_SERVER_ERROR, exception.getErrorCode());
    }

    private PlaceExploreRequest request(
            PlaceExploreScope scope,
            PlaceCategory category,
            String keyword,
            PlaceCoordinate center,
            MapBounds bounds,
            DaejeonHotspot hotspot) {
        return new PlaceExploreRequest(scope, category, keyword, center, bounds, hotspot, null, null);
    }

    private MapBounds bounds() {
        return new MapBounds(
                new BigDecimal("36.31"),
                new BigDecimal("127.42"),
                new BigDecimal("36.34"),
                new BigDecimal("127.45"));
    }

    private KakaoCoordinate coordinate(String longitude, String latitude) {
        return new KakaoCoordinate(new BigDecimal(longitude), new BigDecimal(latitude));
    }

    private KakaoPlace place() {
        return new KakaoPlace(
                "place-1",
                "야구장 근처 식당",
                "음식점 > 한식",
                "FD6",
                "042-123-4567",
                "대전 중구 부사동 1",
                "대전 중구 대종로 1",
                coordinate("127.43", "36.32"),
                "https://place.map.kakao.com/place-1",
                320);
    }
}
