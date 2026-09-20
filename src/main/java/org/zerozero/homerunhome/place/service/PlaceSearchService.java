package org.zerozero.homerunhome.place.service;

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
import org.zerozero.homerunhome.place.dto.PlaceSearchRequest;
import org.zerozero.homerunhome.place.dto.PlaceSearchResponse;

@Service
@RequiredArgsConstructor
public class PlaceSearchService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 15;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final KakaoApiClient kakaoApiClient;

    public PlaceSearchResponse search(PlaceSearchRequest request) {
        if (request == null) {
            throw invalidRequest();
        }
        String keyword = normalizeKeyword(request.keyword());
        PlaceCategory category = validateCategory(request.category());
        int page = request.page() == null ? DEFAULT_PAGE : request.page();
        int size = request.size() == null ? DEFAULT_SIZE : request.size();
        validatePage(page, size);
        KakaoMapBounds bounds = toKakaoBounds(request.bounds());

        if (keyword == null && category == null) {
            throw invalidRequest();
        }
        if (keyword == null && bounds == null) {
            throw invalidRequest();
        }

        KakaoSearchResult<KakaoPlace> result = keyword == null
                ? searchByCategory(category, bounds, page, size)
                : searchByKeyword(keyword, category, bounds, page, size);
        return toResponse(result, page, size);
    }

    private KakaoSearchResult<KakaoPlace> searchByKeyword(
            String keyword, PlaceCategory category, KakaoMapBounds bounds, int page, int size) {
        return kakaoApiClient.searchPlaces(new KakaoPlaceSearchRequest(
                keyword,
                null,
                null,
                category == null ? null : category.kakaoCode(),
                bounds,
                KakaoPlaceSearchSort.ACCURACY,
                page,
                size));
    }

    private KakaoSearchResult<KakaoPlace> searchByCategory(
            PlaceCategory category, KakaoMapBounds bounds, int page, int size) {
        return kakaoApiClient.searchPlacesByCategory(
                new KakaoCategoryPlaceSearchRequest(category.kakaoCode(), bounds, page, size));
    }

    private PlaceSearchResponse toResponse(KakaoSearchResult<KakaoPlace> result, int page, int size) {
        var places = result.items().stream()
                .map(place -> new PlaceResponse(
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
                        place.distanceMeters()))
                .toList();
        return new PlaceSearchResponse(
                places,
                page,
                size,
                result.totalCount(),
                result.pageableCount(),
                !result.end());
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

    private PlaceCategory validateCategory(PlaceCategory category) {
        if (category == PlaceCategory.OTHER) {
            throw invalidRequest();
        }
        return category;
    }

    private void validatePage(int page, int size) {
        if (page < 1 || page > 45 || size < 1 || size > 15) {
            throw invalidRequest();
        }
    }

    private KakaoMapBounds toKakaoBounds(MapBounds bounds) {
        if (bounds == null) {
            return null;
        }
        if (hasNullCoordinate(bounds)) {
            throw invalidRequest();
        }
        try {
            return new KakaoMapBounds(
                    coordinate(bounds.southWestLongitude(), bounds.southWestLatitude()),
                    coordinate(bounds.northEastLongitude(), bounds.northEastLatitude()));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw invalidRequest();
        }
    }

    private boolean hasNullCoordinate(MapBounds bounds) {
        return bounds.southWestLatitude() == null
                || bounds.southWestLongitude() == null
                || bounds.northEastLatitude() == null
                || bounds.northEastLongitude() == null;
    }

    private KakaoCoordinate coordinate(BigDecimal longitude, BigDecimal latitude) {
        return new KakaoCoordinate(longitude, latitude);
    }

    private BusinessException invalidRequest() {
        return new BusinessException(ErrorCode.INVALID_PLACE_SEARCH_REQUEST);
    }
}
