package org.zerozero.homerunhome.place.recommendation;

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
import org.zerozero.homerunhome.place.explore.dto.PlaceCoordinate;
import org.zerozero.homerunhome.place.recommendation.dto.PlaceRecommendationRequest;
import org.zerozero.homerunhome.place.recommendation.service.PlaceRecommendationService;

class PlaceRecommendationServiceTest {

    private static final KakaoCoordinate REFERENCE = coordinate("127.429188", "36.317067");

    private KakaoApiClient kakaoApiClient;
    private PlaceRecommendationService service;

    @BeforeEach
    void setUp() {
        kakaoApiClient = mock(KakaoApiClient.class);
        service = new PlaceRecommendationService(kakaoApiClient);
    }

    @Test
    void recommendsPreferredCategoriesWithoutDuplicatesInDistanceOrder() {
        KakaoCategoryPlaceSearchRequest foodRequest =
                KakaoCategoryPlaceSearchRequest.around("FD6", REFERENCE, 500, 1, 15);
        KakaoCategoryPlaceSearchRequest cafeRequest =
                KakaoCategoryPlaceSearchRequest.around("CE7", REFERENCE, 500, 1, 15);
        when(kakaoApiClient.searchPlacesByCategory(foodRequest))
                .thenReturn(result(List.of(
                        place("food-1", "식당", "FD6", "127.430188", "36.317067"),
                        place("duplicate", "공통 장소", "FD6", "127.429188", "36.317067"),
                        place("far", "먼 장소", "FD6", "127.439188", "36.317067"))));
        when(kakaoApiClient.searchPlacesByCategory(cafeRequest))
                .thenReturn(result(List.of(
                        place("cafe-1", "카페", "CE7", "127.431188", "36.317067"),
                        place("duplicate", "공통 장소", "CE7", "127.429188", "36.317067"))));

        var response = service.recommend(request(
                List.of(PlaceCategory.FOOD, PlaceCategory.CAFE), null, 500, null, 15));

        assertThat(response.categories()).containsExactly(PlaceCategory.FOOD, PlaceCategory.CAFE);
        assertThat(response.places()).extracting(place -> place.id())
                .containsExactly("duplicate", "food-1", "cafe-1");
        assertThat(response.places()).extracting(place -> place.distanceMeters())
                .isSorted();
        verify(kakaoApiClient).searchPlacesByCategory(foodRequest);
        verify(kakaoApiClient).searchPlacesByCategory(cafeRequest);
    }

    @Test
    void appliesKeywordAndCourseBoundsThenFiltersByMaximumDistance() {
        MapBounds bounds = bounds();
        KakaoMapBounds kakaoBounds = new KakaoMapBounds(
                coordinate("127.42", "36.31"), coordinate("127.45", "36.34"));
        KakaoPlaceSearchRequest kakaoRequest = new KakaoPlaceSearchRequest(
                "디저트",
                null,
                null,
                "CE7",
                kakaoBounds,
                KakaoPlaceSearchSort.ACCURACY,
                1,
                15);
        when(kakaoApiClient.searchPlaces(kakaoRequest)).thenReturn(result(List.of(
                place("near", "가까운 디저트", "CE7", "127.430188", "36.317067"),
                place("far", "먼 디저트", "CE7", "127.439188", "36.317067"))));

        var response = service.recommend(request(
                List.of(PlaceCategory.CAFE), "  디저트  ", 500, bounds, 10));

        assertEquals("디저트", response.keyword());
        assertThat(response.places()).extracting(place -> place.id()).containsExactly("near");
        verify(kakaoApiClient).searchPlaces(kakaoRequest);
    }

    @Test
    void removesDuplicatedCategoriesBeforeCallingKakao() {
        KakaoCategoryPlaceSearchRequest kakaoRequest =
                KakaoCategoryPlaceSearchRequest.around("FD6", REFERENCE, 1500, 1, 15);
        when(kakaoApiClient.searchPlacesByCategory(kakaoRequest)).thenReturn(result(List.of()));

        var response = service.recommend(request(
                List.of(PlaceCategory.FOOD, PlaceCategory.FOOD), null, null, null, null));

        assertThat(response.categories()).containsExactly(PlaceCategory.FOOD);
        verify(kakaoApiClient).searchPlacesByCategory(kakaoRequest);
    }

    @Test
    void rejectsMissingReferencePoint() {
        PlaceRecommendationRequest request = new PlaceRecommendationRequest(
                List.of(PlaceCategory.FOOD), null, null, 1000, null, 10);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.recommend(request));

        assertEquals(ErrorCode.INVALID_PLACE_RECOMMENDATION_REQUEST, exception.getErrorCode());
        verify(kakaoApiClient, never()).searchPlacesByCategory(foodRequest());
    }

    @Test
    void rejectsUnsupportedCategoryAndInvalidDistance() {
        BusinessException categoryException = assertThrows(
                BusinessException.class,
                () -> service.recommend(request(List.of(PlaceCategory.OTHER), null, 1000, null, 10)));
        BusinessException distanceException = assertThrows(
                BusinessException.class,
                () -> service.recommend(request(List.of(PlaceCategory.FOOD), null, 20_001, null, 10)));

        assertEquals(ErrorCode.INVALID_PLACE_RECOMMENDATION_REQUEST, categoryException.getErrorCode());
        assertEquals(ErrorCode.INVALID_PLACE_RECOMMENDATION_REQUEST, distanceException.getErrorCode());
    }

    @Test
    void keepsExternalApiFailureDistinct() {
        when(kakaoApiClient.searchPlacesByCategory(foodRequest()))
                .thenThrow(new BusinessException(ErrorCode.KAKAO_EXTERNAL_SERVER_ERROR));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.recommend(request(List.of(PlaceCategory.FOOD), null, 1500, null, 15)));

        assertEquals(ErrorCode.KAKAO_EXTERNAL_SERVER_ERROR, exception.getErrorCode());
    }

    private PlaceRecommendationRequest request(
            List<PlaceCategory> categories,
            String keyword,
            Integer maxDistanceMeters,
            MapBounds bounds,
            Integer limit) {
        return new PlaceRecommendationRequest(
                categories,
                keyword,
                new PlaceCoordinate(REFERENCE.latitude(), REFERENCE.longitude()),
                maxDistanceMeters,
                bounds,
                limit);
    }

    private KakaoCategoryPlaceSearchRequest foodRequest() {
        return KakaoCategoryPlaceSearchRequest.around("FD6", REFERENCE, 1500, 1, 15);
    }

    private MapBounds bounds() {
        return new MapBounds(
                new BigDecimal("36.31"),
                new BigDecimal("127.42"),
                new BigDecimal("36.34"),
                new BigDecimal("127.45"));
    }

    private KakaoSearchResult<KakaoPlace> result(List<KakaoPlace> places) {
        return new KakaoSearchResult<>(places, places.size(), places.size(), true);
    }

    private KakaoPlace place(
            String id, String name, String categoryCode, String longitude, String latitude) {
        return new KakaoPlace(
                id,
                name,
                "분류",
                categoryCode,
                "042-123-4567",
                "대전광역시",
                "대전광역시 도로명",
                coordinate(longitude, latitude),
                "https://place.map.kakao.com/" + id,
                null);
    }

    private static KakaoCoordinate coordinate(String longitude, String latitude) {
        return new KakaoCoordinate(new BigDecimal(longitude), new BigDecimal(latitude));
    }
}
