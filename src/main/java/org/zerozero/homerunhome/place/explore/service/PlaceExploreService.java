package org.zerozero.homerunhome.place.explore.service;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
import org.zerozero.homerunhome.place.dto.PlaceResponse;
import org.zerozero.homerunhome.place.explore.dto.DaejeonHotspot;
import org.zerozero.homerunhome.place.explore.dto.PlaceCoordinate;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreRequest;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreResponse;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreScope;

@Service
@RequiredArgsConstructor
public class PlaceExploreService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 15;
    private static final int COURSE_RADIUS_METERS = 1000;
    private static final int STADIUM_RADIUS_METERS = 1500;
    private static final int STATION_RADIUS_METERS = 1000;
    private static final int CURRENT_LOCATION_RADIUS_METERS = 1000;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final KakaoCoordinate STADIUM = coordinate("127.429188", "36.317067");
    private static final KakaoCoordinate DAEJEON_STATION = coordinate("127.4345", "36.3322");

    private final KakaoApiClient kakaoApiClient;

    public PlaceExploreResponse explore(PlaceExploreRequest request) {
        validateRequiredFields(request);
        int page = request.page() == null ? DEFAULT_PAGE : request.page();
        int size = request.size() == null ? DEFAULT_SIZE : request.size();
        validatePage(page, size);
        String keyword = normalizeKeyword(request.keyword());
        SearchArea area = resolveArea(request);

        KakaoSearchResult<KakaoPlace> result = keyword == null
                ? searchByCategory(request.category(), area, page, size)
                : searchByKeyword(keyword, request.category(), area, page, size);
        return toResponse(request.scope(), request.hotspot(), result, page, size);
    }

    private void validateRequiredFields(PlaceExploreRequest request) {
        if (request == null
                || request.scope() == null
                || request.category() == null
                || !request.category().searchable()) {
            throw invalidRequest();
        }
    }

    private SearchArea resolveArea(PlaceExploreRequest request) {
        return switch (request.scope()) {
            case STADIUM_NEARBY -> SearchArea.around(STADIUM, STADIUM_RADIUS_METERS);
            case STATION_NEARBY -> SearchArea.around(DAEJEON_STATION, STATION_RADIUS_METERS);
            case CURRENT_LOCATION -> SearchArea.around(
                    toKakaoCoordinate(request.center()), CURRENT_LOCATION_RADIUS_METERS);
            case DAEJEON_HOTSPOT -> hotspotArea(request.hotspot());
            case CUSTOM -> SearchArea.inside(toKakaoBounds(request.bounds()));
            case COURSE_NEARBY -> courseArea(request);
        };
    }

    private SearchArea courseArea(PlaceExploreRequest request) {
        if (request.bounds() != null) {
            return SearchArea.inside(toKakaoBounds(request.bounds()));
        }
        return SearchArea.around(toKakaoCoordinate(request.center()), COURSE_RADIUS_METERS);
    }

    private SearchArea hotspotArea(DaejeonHotspot hotspot) {
        if (hotspot == null) {
            throw invalidRequest();
        }
        return SearchArea.around(
                new KakaoCoordinate(hotspot.longitude(), hotspot.latitude()),
                hotspot.radiusMeters());
    }

    private KakaoSearchResult<KakaoPlace> searchByCategory(
            PlaceCategory category, SearchArea area, int page, int size) {
        KakaoCategoryPlaceSearchRequest request = area.bounds() == null
                ? KakaoCategoryPlaceSearchRequest.around(
                        category.kakaoCode(), area.center(), area.radiusMeters(), page, size)
                : new KakaoCategoryPlaceSearchRequest(category.kakaoCode(), area.bounds(), page, size);
        return kakaoApiClient.searchPlacesByCategory(request);
    }

    private KakaoSearchResult<KakaoPlace> searchByKeyword(
            String keyword, PlaceCategory category, SearchArea area, int page, int size) {
        return kakaoApiClient.searchPlaces(new KakaoPlaceSearchRequest(
                keyword,
                area.center(),
                area.radiusMeters(),
                category.kakaoCode(),
                area.bounds(),
                area.center() == null ? KakaoPlaceSearchSort.ACCURACY : KakaoPlaceSearchSort.DISTANCE,
                page,
                size));
    }

    private PlaceExploreResponse toResponse(
            PlaceExploreScope scope,
            DaejeonHotspot hotspot,
            KakaoSearchResult<KakaoPlace> result,
            int page,
            int size) {
        var places = result.items().stream().map(this::toPlaceResponse).toList();
        return new PlaceExploreResponse(
                scope,
                scope == PlaceExploreScope.DAEJEON_HOTSPOT ? hotspot : null,
                places,
                page,
                size,
                result.totalCount(),
                result.pageableCount(),
                !result.end());
    }

    private PlaceResponse toPlaceResponse(KakaoPlace place) {
        return new PlaceResponse(
                place.id(),
                place.name(),
                place.addressName(),
                place.roadAddressName(),
                place.coordinate().latitude(),
                place.coordinate().longitude(),
                PlaceCategory.fromKakaoCode(place.categoryGroupCode()),
                place.categoryName(),
                place.phone(),
                place.placeUrl(),
                place.distanceMeters());
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.length() > MAX_KEYWORD_LENGTH
                || trimmed.codePoints().anyMatch(Character::isISOControl)) {
            throw invalidRequest();
        }
        return WHITESPACE.matcher(trimmed).replaceAll(" ");
    }

    private void validatePage(int page, int size) {
        if (page < 1 || page > 45 || size < 1 || size > 15) {
            throw invalidRequest();
        }
    }

    private KakaoCoordinate toKakaoCoordinate(PlaceCoordinate coordinate) {
        if (coordinate == null) {
            throw invalidRequest();
        }
        try {
            return new KakaoCoordinate(coordinate.longitude(), coordinate.latitude());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw invalidRequest();
        }
    }

    private KakaoMapBounds toKakaoBounds(MapBounds bounds) {
        if (bounds == null) {
            throw invalidRequest();
        }
        try {
            return new KakaoMapBounds(
                    new KakaoCoordinate(bounds.southWestLongitude(), bounds.southWestLatitude()),
                    new KakaoCoordinate(bounds.northEastLongitude(), bounds.northEastLatitude()));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw invalidRequest();
        }
    }

    private BusinessException invalidRequest() {
        return new BusinessException(ErrorCode.INVALID_PLACE_EXPLORATION_REQUEST);
    }

    private static KakaoCoordinate coordinate(String longitude, String latitude) {
        return new KakaoCoordinate(new BigDecimal(longitude), new BigDecimal(latitude));
    }

    private record SearchArea(KakaoCoordinate center, Integer radiusMeters, KakaoMapBounds bounds) {

        private static SearchArea around(KakaoCoordinate center, int radiusMeters) {
            return new SearchArea(center, radiusMeters, null);
        }

        private static SearchArea inside(KakaoMapBounds bounds) {
            return new SearchArea(null, null, bounds);
        }
    }
}
