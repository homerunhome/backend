package org.zerozero.homerunhome.place.recommendation.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.zerozero.homerunhome.place.explore.dto.PlaceCoordinate;
import org.zerozero.homerunhome.place.recommendation.dto.PlaceRecommendationRequest;
import org.zerozero.homerunhome.place.recommendation.dto.PlaceRecommendationResponse;

@Service
@RequiredArgsConstructor
public class PlaceRecommendationService {

    private static final int DEFAULT_MAX_DISTANCE_METERS = 1500;
    private static final int DEFAULT_LIMIT = 15;
    private static final int MAX_LIMIT = 15;
    private static final int MAX_CATEGORY_COUNT = 5;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final KakaoApiClient kakaoApiClient;

    public PlaceRecommendationResponse recommend(PlaceRecommendationRequest request) {
        if (request == null) {
            throw invalidRequest();
        }

        List<PlaceCategory> categories = validateCategories(request.categories());
        String keyword = normalizeKeyword(request.keyword());
        KakaoCoordinate referencePoint = toKakaoCoordinate(request.referencePoint());
        int maxDistanceMeters = request.maxDistanceMeters() == null
                ? DEFAULT_MAX_DISTANCE_METERS
                : request.maxDistanceMeters();
        int limit = request.limit() == null ? DEFAULT_LIMIT : request.limit();
        validateDistanceAndLimit(maxDistanceMeters, limit);
        KakaoMapBounds courseBounds = toKakaoBounds(request.courseBounds());

        Map<String, PlaceResponse> uniquePlaces = new LinkedHashMap<>();
        for (PlaceCategory category : categories) {
            search(category, keyword, referencePoint, maxDistanceMeters, courseBounds).items().stream()
                    .map(place -> toPlaceResponse(place, referencePoint))
                    .filter(place -> place.distanceMeters() <= maxDistanceMeters)
                    .forEach(place -> uniquePlaces.merge(
                            place.id(), place, PlaceRecommendationService::nearerPlace));
        }

        List<PlaceResponse> places = uniquePlaces.values().stream()
                .sorted(Comparator.comparingInt(PlaceResponse::distanceMeters))
                .limit(limit)
                .toList();
        return new PlaceRecommendationResponse(
                categories,
                keyword,
                request.referencePoint(),
                maxDistanceMeters,
                places);
    }

    private KakaoSearchResult<KakaoPlace> search(
            PlaceCategory category,
            String keyword,
            KakaoCoordinate referencePoint,
            int maxDistanceMeters,
            KakaoMapBounds courseBounds) {
        if (keyword == null) {
            KakaoCategoryPlaceSearchRequest request = courseBounds == null
                    ? KakaoCategoryPlaceSearchRequest.around(
                            category.kakaoCode(), referencePoint, maxDistanceMeters, 1, MAX_LIMIT)
                    : new KakaoCategoryPlaceSearchRequest(category.kakaoCode(), courseBounds, 1, MAX_LIMIT);
            return kakaoApiClient.searchPlacesByCategory(request);
        }

        KakaoPlaceSearchRequest request = courseBounds == null
                ? new KakaoPlaceSearchRequest(
                        keyword,
                        referencePoint,
                        maxDistanceMeters,
                        category.kakaoCode(),
                        null,
                        KakaoPlaceSearchSort.DISTANCE,
                        1,
                        MAX_LIMIT)
                : new KakaoPlaceSearchRequest(
                        keyword,
                        null,
                        null,
                        category.kakaoCode(),
                        courseBounds,
                        KakaoPlaceSearchSort.ACCURACY,
                        1,
                        MAX_LIMIT);
        return kakaoApiClient.searchPlaces(request);
    }

    private List<PlaceCategory> validateCategories(List<PlaceCategory> categories) {
        if (categories == null || categories.isEmpty() || categories.size() > MAX_CATEGORY_COUNT) {
            throw invalidRequest();
        }
        List<PlaceCategory> distinctCategories = categories.stream().distinct().toList();
        if (distinctCategories.stream().anyMatch(category -> category == null || !category.searchable())) {
            throw invalidRequest();
        }
        return distinctCategories;
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

    private void validateDistanceAndLimit(int maxDistanceMeters, int limit) {
        if (maxDistanceMeters < 1 || maxDistanceMeters > 20_000 || limit < 1 || limit > MAX_LIMIT) {
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
            return null;
        }
        try {
            return new KakaoMapBounds(
                    new KakaoCoordinate(bounds.southWestLongitude(), bounds.southWestLatitude()),
                    new KakaoCoordinate(bounds.northEastLongitude(), bounds.northEastLatitude()));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw invalidRequest();
        }
    }

    private PlaceResponse toPlaceResponse(KakaoPlace place, KakaoCoordinate referencePoint) {
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
                distanceBetween(referencePoint, place.coordinate()));
    }

    private int distanceBetween(KakaoCoordinate from, KakaoCoordinate to) {
        double fromLatitude = Math.toRadians(from.latitude().doubleValue());
        double toLatitude = Math.toRadians(to.latitude().doubleValue());
        double latitudeDelta = toLatitude - fromLatitude;
        double longitudeDelta = Math.toRadians(to.longitude().doubleValue() - from.longitude().doubleValue());
        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(fromLatitude) * Math.cos(toLatitude) * Math.pow(Math.sin(longitudeDelta / 2), 2);
        double centralAngle = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return (int) Math.round(EARTH_RADIUS_METERS * centralAngle);
    }

    private static PlaceResponse nearerPlace(PlaceResponse first, PlaceResponse second) {
        return first.distanceMeters() <= second.distanceMeters() ? first : second;
    }

    private BusinessException invalidRequest() {
        return new BusinessException(ErrorCode.INVALID_PLACE_RECOMMENDATION_REQUEST);
    }
}
