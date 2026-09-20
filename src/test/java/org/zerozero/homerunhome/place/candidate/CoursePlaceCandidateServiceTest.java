package org.zerozero.homerunhome.place.candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.kakao.client.KakaoApiClient;
import org.zerozero.homerunhome.kakao.dto.common.KakaoCoordinate;
import org.zerozero.homerunhome.kakao.dto.common.KakaoSearchResult;
import org.zerozero.homerunhome.kakao.dto.request.KakaoCategoryPlaceSearchRequest;
import org.zerozero.homerunhome.kakao.dto.response.KakaoPlace;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCandidateRequest;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCategory;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceProvider;
import org.zerozero.homerunhome.place.candidate.service.CoursePlaceCandidateAssembler;
import org.zerozero.homerunhome.place.candidate.service.CoursePlaceCandidateService;
import org.zerozero.homerunhome.place.explore.dto.PlaceCoordinate;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreScope;
import org.zerozero.homerunhome.tourapi.client.KoreanTourApiClient;
import org.zerozero.homerunhome.tourapi.client.TourApiPageResult;
import org.zerozero.homerunhome.tourapi.dto.TourApiContent;

class CoursePlaceCandidateServiceTest {

    private static final PlaceCoordinate STADIUM =
            coordinate("36.317067", "127.429188");

    private KakaoApiClient kakaoClient;
    private KoreanTourApiClient tourClient;
    private CoursePlaceCandidateService service;

    @BeforeEach
    void setUp() {
        kakaoClient = mock(KakaoApiClient.class);
        tourClient = mock(KoreanTourApiClient.class);
        service = new CoursePlaceCandidateService(
                kakaoClient, tourClient, new CoursePlaceCandidateAssembler());
    }

    @Test
    void combinesKakaoAndTourApiCandidatesInDistanceOrder() {
        KakaoCategoryPlaceSearchRequest foodRequest = KakaoCategoryPlaceSearchRequest.around(
                "FD6", toKakao(STADIUM), 1500, 1, 15);
        when(kakaoClient.searchPlacesByCategory(foodRequest))
                .thenReturn(kakaoResult(List.of(kakaoPlace(
                        "food-1", "대전 맛집", "FD6", "127.430188", "36.317067"))));
        when(kakaoClient.searchPlacesByCategory(KakaoCategoryPlaceSearchRequest.around(
                        "AT4", toKakao(STADIUM), 1500, 1, 15)))
                .thenReturn(kakaoResult(List.of()));
        when(tourClient.findNearbyContents(
                        "12", STADIUM.longitude(), STADIUM.latitude(), 1500, 1, 100))
                .thenReturn(tourResult(List.of(tourContent(
                        "tour-1", "12", "대전 관광지", "127.431188", "36.317067", null))));

        var response = service.findCandidates(request(
                List.of(CoursePlaceCategory.FOOD, CoursePlaceCategory.TOURIST_ATTRACTION),
                null,
                1500,
                null,
                30));

        assertEquals(PlaceExploreScope.STADIUM_NEARBY, response.scope());
        assertEquals(STADIUM, response.center());
        assertThat(response.places()).extracting(place -> place.externalId())
                .containsExactly("food-1", "tour-1");
        assertThat(response.places()).extracting(place -> place.provider())
                .containsExactly(CoursePlaceProvider.KAKAO, CoursePlaceProvider.TOUR_API);
    }

    @Test
    void removesSameNamedNearbyPlaceAcrossProvidersAndKeepsImage() {
        KakaoCategoryPlaceSearchRequest request = KakaoCategoryPlaceSearchRequest.around(
                "AT4", toKakao(STADIUM), 1500, 1, 15);
        when(kakaoClient.searchPlacesByCategory(request))
                .thenReturn(kakaoResult(List.of(kakaoPlace(
                        "kakao-1", "한밭 수목원", "AT4", "127.430000", "36.317067"))));
        when(tourClient.findNearbyContents(
                        "12", STADIUM.longitude(), STADIUM.latitude(), 1500, 1, 100))
                .thenReturn(tourResult(List.of(tourContent(
                        "tour-1", "12", "한밭수목원", "127.430100", "36.317067", "image.jpg"))));

        var response = service.findCandidates(request(
                List.of(CoursePlaceCategory.TOURIST_ATTRACTION), null, 1500, null, 30));

        assertThat(response.places()).singleElement().satisfies(place -> {
            assertEquals(CoursePlaceProvider.TOUR_API, place.provider());
            assertEquals("tour-1", place.externalId());
            assertEquals("image.jpg", place.imageUrl());
        });
    }

    @Test
    void filtersEventsByDateKeywordAndDistance() {
        LocalDate gameDate = LocalDate.of(2026, 9, 20);
        when(tourClient.findDaejeonEvents(gameDate, gameDate, 1, 100))
                .thenReturn(tourResult(List.of(
                        tourContent("near", "15", "대전 야구 축제", "127.430188", "36.317067", null),
                        tourContent("other", "15", "대전 음악회", "127.430188", "36.317067", null),
                        tourContent("far", "15", "야구 행사", "127.459188", "36.317067", null))));

        var response = service.findCandidates(request(
                List.of(CoursePlaceCategory.FESTIVAL_EVENT), "야구", 1500, gameDate, 30));

        assertThat(response.places()).singleElement()
                .satisfies(place -> {
                    assertEquals("near", place.externalId());
                    assertEquals(gameDate, place.eventStartDate());
                    assertEquals(gameDate, place.eventEndDate());
                    assertEquals("042-123-4567", place.phone());
                });
    }

    @Test
    void searchesAroundEveryExistingCoursePoint() {
        PlaceCoordinate first = coordinate("36.317067", "127.429188");
        PlaceCoordinate second = coordinate("36.3322", "127.4345");
        when(kakaoClient.searchPlacesByCategory(KakaoCategoryPlaceSearchRequest.around(
                        "FD6", toKakao(first), 1000, 1, 15)))
                .thenReturn(kakaoResult(List.of(kakaoPlace(
                        "first", "첫 번째 장소 주변", "FD6", "127.430188", "36.317067"))));
        when(kakaoClient.searchPlacesByCategory(KakaoCategoryPlaceSearchRequest.around(
                        "FD6", toKakao(second), 1000, 1, 15)))
                .thenReturn(kakaoResult(List.of(kakaoPlace(
                        "second", "두 번째 장소 주변", "FD6", "127.4355", "36.3322"))));
        CoursePlaceCandidateRequest request = new CoursePlaceCandidateRequest(
                PlaceExploreScope.COURSE_NEARBY,
                List.of(CoursePlaceCategory.FOOD),
                null,
                List.of(first, second),
                null,
                null,
                1000,
                null,
                20);

        var response = service.findCandidates(request);

        assertThat(response.searchCenters()).containsExactly(first, second);
        assertThat(response.places()).extracting(place -> place.externalId())
                .containsExactlyInAnyOrder("first", "second");
    }

    @Test
    void loadsAdditionalKakaoPagesWhenLimitExceedsOnePage() {
        KakaoCategoryPlaceSearchRequest firstPage = KakaoCategoryPlaceSearchRequest.around(
                "FD6", toKakao(STADIUM), 1500, 1, 15);
        KakaoCategoryPlaceSearchRequest secondPage = KakaoCategoryPlaceSearchRequest.around(
                "FD6", toKakao(STADIUM), 1500, 2, 15);
        when(kakaoClient.searchPlacesByCategory(firstPage))
                .thenReturn(new KakaoSearchResult<>(
                        List.of(kakaoPlace("page-1", "첫 페이지", "FD6", "127.430", "36.317067")),
                        2,
                        2,
                        false));
        when(kakaoClient.searchPlacesByCategory(secondPage))
                .thenReturn(kakaoResult(List.of(kakaoPlace(
                        "page-2", "두 번째 페이지", "FD6", "127.431", "36.317067"))));

        var response = service.findCandidates(request(
                List.of(CoursePlaceCategory.FOOD), null, 1500, null, 30));

        assertThat(response.places()).extracting(place -> place.externalId())
                .containsExactly("page-1", "page-2");
        verify(kakaoClient).searchPlacesByCategory(secondPage);
    }

    @Test
    void requiresCenterForCurrentLocationAndDateForEvents() {
        CoursePlaceCandidateRequest missingCenter = new CoursePlaceCandidateRequest(
                PlaceExploreScope.CURRENT_LOCATION,
                List.of(CoursePlaceCategory.CAFE),
                null,
                null,
                null,
                1000,
                null,
                10);
        CoursePlaceCandidateRequest missingDate = request(
                List.of(CoursePlaceCategory.FESTIVAL_EVENT), null, 1000, null, 10);

        BusinessException centerError = assertThrows(
                BusinessException.class, () -> service.findCandidates(missingCenter));
        BusinessException dateError = assertThrows(
                BusinessException.class, () -> service.findCandidates(missingDate));

        assertEquals(ErrorCode.INVALID_COURSE_PLACE_CANDIDATE_REQUEST, centerError.getErrorCode());
        assertEquals(ErrorCode.INVALID_COURSE_PLACE_CANDIDATE_REQUEST, dateError.getErrorCode());
        verify(kakaoClient, never()).searchPlacesByCategory(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void keepsExternalApiFailureDistinct() {
        when(kakaoClient.searchPlacesByCategory(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.KAKAO_EXTERNAL_SERVER_ERROR));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.findCandidates(request(
                        List.of(CoursePlaceCategory.FOOD), null, 1500, null, 10)));

        assertEquals(ErrorCode.KAKAO_EXTERNAL_SERVER_ERROR, exception.getErrorCode());
    }

    private CoursePlaceCandidateRequest request(
            List<CoursePlaceCategory> categories,
            String keyword,
            Integer distance,
            LocalDate eventDate,
            Integer limit) {
        return new CoursePlaceCandidateRequest(
                PlaceExploreScope.STADIUM_NEARBY,
                categories,
                null,
                null,
                keyword,
                distance,
                eventDate,
                limit);
    }

    private KakaoSearchResult<KakaoPlace> kakaoResult(List<KakaoPlace> places) {
        return new KakaoSearchResult<>(places, places.size(), places.size(), true);
    }

    private TourApiPageResult<TourApiContent> tourResult(List<TourApiContent> contents) {
        return new TourApiPageResult<>(contents, 1, 100, contents.size());
    }

    private KakaoPlace kakaoPlace(
            String id, String name, String category, String longitude, String latitude) {
        return new KakaoPlace(
                id,
                name,
                "분류",
                category,
                "042-123-4567",
                "대전 주소",
                "대전 도로명",
                new KakaoCoordinate(new BigDecimal(longitude), new BigDecimal(latitude)),
                "https://place.map.kakao.com/" + id,
                null);
    }

    private TourApiContent tourContent(
            String id,
            String type,
            String title,
            String longitude,
            String latitude,
            String image) {
        return new TourApiContent(
                id,
                type,
                title,
                "대전 주소",
                "",
                "042-123-4567",
                "",
                "",
                image,
                null,
                new BigDecimal(longitude),
                new BigDecimal(latitude),
                "20260920",
                "20260920");
    }

    private static PlaceCoordinate coordinate(String latitude, String longitude) {
        return new PlaceCoordinate(new BigDecimal(latitude), new BigDecimal(longitude));
    }

    private static KakaoCoordinate toKakao(PlaceCoordinate coordinate) {
        return new KakaoCoordinate(coordinate.longitude(), coordinate.latitude());
    }
}
