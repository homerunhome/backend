package org.zerozero.homerunhome.place.candidate.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.kakao.client.KakaoApiClient;
import org.zerozero.homerunhome.kakao.dto.common.KakaoCoordinate;
import org.zerozero.homerunhome.kakao.dto.request.KakaoCategoryPlaceSearchRequest;
import org.zerozero.homerunhome.kakao.dto.request.KakaoPlaceSearchRequest;
import org.zerozero.homerunhome.kakao.dto.request.KakaoPlaceSearchSort;
import org.zerozero.homerunhome.kakao.dto.response.KakaoPlace;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCandidate;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCandidateRequest;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCandidateResponse;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCategory;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceProvider;
import org.zerozero.homerunhome.place.explore.dto.DaejeonHotspot;
import org.zerozero.homerunhome.place.explore.dto.PlaceCoordinate;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreScope;
import org.zerozero.homerunhome.tourapi.client.KoreanTourApiClient;
import org.zerozero.homerunhome.tourapi.dto.TourApiContent;

@Service
@RequiredArgsConstructor
public class CoursePlaceCandidateService {

    private static final int DEFAULT_LIMIT = 30;
    private static final int MAX_LIMIT = 50;
    private static final int MAX_DISTANCE_METERS = 20_000;
    private static final int KAKAO_PAGE_SIZE = 15;
    private static final int TOUR_API_PAGE_SIZE = 100;
    private static final int DUPLICATE_DISTANCE_METERS = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern NAME_SEPARATOR = Pattern.compile("[\\p{Punct}\\s]+");
    private static final PlaceCoordinate STADIUM = coordinate("36.317067", "127.429188");
    private static final PlaceCoordinate DAEJEON_STATION = coordinate("36.3322", "127.4345");

    private final KakaoApiClient kakaoApiClient;
    private final KoreanTourApiClient koreanTourApiClient;

    public CoursePlaceCandidateResponse findCandidates(CoursePlaceCandidateRequest request) {
        validateRequired(request);
        List<CoursePlaceCategory> categories = request.categories().stream().distinct().toList();
        String keyword = normalizeKeyword(request.keyword());
        PlaceCoordinate center = resolveCenter(request);
        int maxDistanceMeters = request.maxDistanceMeters() == null
                ? defaultDistance(request.scope(), request.hotspot())
                : request.maxDistanceMeters();
        int limit = request.limit() == null ? DEFAULT_LIMIT : request.limit();
        validateOptions(categories, center, maxDistanceMeters, limit, request.eventDate());

        List<CoursePlaceCandidate> candidates = new ArrayList<>();
        for (CoursePlaceCategory category : categories) {
            if (category.usesKakao()) {
                candidates.addAll(findKakaoCandidates(
                        category, keyword, center, maxDistanceMeters));
            }
            if (category.usesNearbyTourApi()) {
                candidates.addAll(findTourCandidates(
                        category, keyword, center, maxDistanceMeters));
            }
            if (category == CoursePlaceCategory.FESTIVAL_EVENT) {
                candidates.addAll(findEventCandidates(
                        keyword, center, maxDistanceMeters, request.eventDate()));
            }
        }

        List<CoursePlaceCandidate> places = removeDuplicates(candidates).stream()
                .sorted(Comparator.comparingInt(CoursePlaceCandidate::distanceMeters))
                .limit(limit)
                .toList();
        return new CoursePlaceCandidateResponse(request.scope(), center, maxDistanceMeters, places);
    }

    private List<CoursePlaceCandidate> findKakaoCandidates(
            CoursePlaceCategory category,
            String keyword,
            PlaceCoordinate center,
            int maxDistanceMeters) {
        KakaoCoordinate kakaoCenter = toKakaoCoordinate(center);
        String query = keyword == null ? category.defaultKeyword() : keyword;
        List<KakaoPlace> places;
        if (query == null) {
            places = kakaoApiClient.searchPlacesByCategory(KakaoCategoryPlaceSearchRequest.around(
                            category.kakaoCode(), kakaoCenter, maxDistanceMeters, 1, KAKAO_PAGE_SIZE))
                    .items();
        } else {
            places = kakaoApiClient.searchPlaces(new KakaoPlaceSearchRequest(
                            query,
                            kakaoCenter,
                            maxDistanceMeters,
                            category.kakaoCode(),
                            null,
                            KakaoPlaceSearchSort.DISTANCE,
                            1,
                            KAKAO_PAGE_SIZE))
                    .items();
        }
        return places.stream()
                .filter(place -> place.coordinate() != null)
                .map(place -> toCandidate(place, category, center))
                .filter(place -> place.distanceMeters() <= maxDistanceMeters)
                .toList();
    }

    private List<CoursePlaceCandidate> findTourCandidates(
            CoursePlaceCategory category,
            String keyword,
            PlaceCoordinate center,
            int maxDistanceMeters) {
        return koreanTourApiClient
                .findNearbyContents(
                        category.tourismContentType().tourApiId(),
                        center.longitude(),
                        center.latitude(),
                        maxDistanceMeters,
                        1,
                        TOUR_API_PAGE_SIZE)
                .items().stream()
                .filter(this::hasCoordinate)
                .filter(content -> matchesKeyword(content, keyword))
                .map(content -> toCandidate(content, category, center))
                .filter(place -> place.distanceMeters() <= maxDistanceMeters)
                .toList();
    }

    private List<CoursePlaceCandidate> findEventCandidates(
            String keyword,
            PlaceCoordinate center,
            int maxDistanceMeters,
            LocalDate eventDate) {
        return koreanTourApiClient
                .findDaejeonEvents(eventDate, eventDate, 1, TOUR_API_PAGE_SIZE)
                .items().stream()
                .filter(this::hasCoordinate)
                .filter(content -> matchesKeyword(content, keyword))
                .map(content -> toCandidate(
                        content, CoursePlaceCategory.FESTIVAL_EVENT, center))
                .filter(place -> place.distanceMeters() <= maxDistanceMeters)
                .toList();
    }

    private List<CoursePlaceCandidate> removeDuplicates(List<CoursePlaceCandidate> candidates) {
        List<CoursePlaceCandidate> unique = new ArrayList<>();
        for (CoursePlaceCandidate candidate : candidates) {
            int duplicateIndex = findDuplicateIndex(unique, candidate);
            if (duplicateIndex < 0) {
                unique.add(candidate);
            } else if (prefer(candidate, unique.get(duplicateIndex))) {
                unique.set(duplicateIndex, candidate);
            }
        }
        return unique;
    }

    private int findDuplicateIndex(
            List<CoursePlaceCandidate> unique, CoursePlaceCandidate candidate) {
        String candidateName = normalizeName(candidate.name());
        if (candidateName.isEmpty()) {
            return -1;
        }
        for (int index = 0; index < unique.size(); index++) {
            CoursePlaceCandidate existing = unique.get(index);
            if (candidateName.equals(normalizeName(existing.name()))
                    && distanceBetween(
                                    candidate.latitude(),
                                    candidate.longitude(),
                                    existing.latitude(),
                                    existing.longitude())
                            <= DUPLICATE_DISTANCE_METERS) {
                return index;
            }
        }
        return -1;
    }

    private boolean prefer(CoursePlaceCandidate candidate, CoursePlaceCandidate existing) {
        boolean candidateHasImage = hasText(candidate.imageUrl());
        boolean existingHasImage = hasText(existing.imageUrl());
        if (candidateHasImage != existingHasImage) {
            return candidateHasImage;
        }
        return candidate.distanceMeters() < existing.distanceMeters();
    }

    private CoursePlaceCandidate toCandidate(
            KakaoPlace place, CoursePlaceCategory category, PlaceCoordinate center) {
        return new CoursePlaceCandidate(
                CoursePlaceProvider.KAKAO,
                place.id(),
                place.name(),
                category,
                hasText(place.roadAddressName()) ? place.roadAddressName() : place.addressName(),
                place.coordinate().latitude(),
                place.coordinate().longitude(),
                null,
                place.placeUrl(),
                distanceBetween(center, place.coordinate().latitude(), place.coordinate().longitude()));
    }

    private CoursePlaceCandidate toCandidate(
            TourApiContent content, CoursePlaceCategory category, PlaceCoordinate center) {
        String address = Stream.of(content.address(), content.addressDetail())
                .filter(this::hasText)
                .reduce((first, second) -> first + " " + second)
                .orElse(null);
        return new CoursePlaceCandidate(
                CoursePlaceProvider.TOUR_API,
                content.contentId(),
                content.title(),
                category,
                address,
                content.latitude(),
                content.longitude(),
                hasText(content.imageUrl()) ? content.imageUrl() : content.thumbnailUrl(),
                "/api/tourism/contents/" + content.contentId(),
                distanceBetween(center, content.latitude(), content.longitude()));
    }

    private PlaceCoordinate resolveCenter(CoursePlaceCandidateRequest request) {
        return switch (request.scope()) {
            case STADIUM_NEARBY -> STADIUM;
            case STATION_NEARBY -> DAEJEON_STATION;
            case DAEJEON_HOTSPOT -> hotspotCoordinate(request.hotspot());
            case CURRENT_LOCATION, COURSE_NEARBY, CUSTOM -> request.center();
        };
    }

    private int defaultDistance(PlaceExploreScope scope, DaejeonHotspot hotspot) {
        return switch (scope) {
            case STADIUM_NEARBY -> 1500;
            case STATION_NEARBY, CURRENT_LOCATION, COURSE_NEARBY, CUSTOM -> 1000;
            case DAEJEON_HOTSPOT -> hotspot == null ? 1000 : hotspot.radiusMeters();
        };
    }

    private PlaceCoordinate hotspotCoordinate(DaejeonHotspot hotspot) {
        return hotspot == null ? null : new PlaceCoordinate(hotspot.latitude(), hotspot.longitude());
    }

    private void validateRequired(CoursePlaceCandidateRequest request) {
        if (request == null
                || request.scope() == null
                || request.categories() == null
                || request.categories().isEmpty()
                || request.categories().stream().anyMatch(category -> category == null)) {
            throw invalidRequest();
        }
    }

    private void validateOptions(
            List<CoursePlaceCategory> categories,
            PlaceCoordinate center,
            int maxDistanceMeters,
            int limit,
            LocalDate eventDate) {
        if (categories.size() > CoursePlaceCategory.values().length
                || !validCoordinate(center)
                || maxDistanceMeters < 1
                || maxDistanceMeters > MAX_DISTANCE_METERS
                || limit < 1
                || limit > MAX_LIMIT
                || (categories.contains(CoursePlaceCategory.FESTIVAL_EVENT) && eventDate == null)) {
            throw invalidRequest();
        }
    }

    private boolean validCoordinate(PlaceCoordinate coordinate) {
        if (coordinate == null || coordinate.latitude() == null || coordinate.longitude() == null) {
            return false;
        }
        try {
            new KakaoCoordinate(coordinate.longitude(), coordinate.latitude());
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private KakaoCoordinate toKakaoCoordinate(PlaceCoordinate coordinate) {
        return new KakaoCoordinate(coordinate.longitude(), coordinate.latitude());
    }

    private String normalizeKeyword(String keyword) {
        if (!hasText(keyword)) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.length() > MAX_KEYWORD_LENGTH
                || trimmed.codePoints().anyMatch(Character::isISOControl)) {
            throw invalidRequest();
        }
        return WHITESPACE.matcher(trimmed).replaceAll(" ");
    }

    private boolean matchesKeyword(TourApiContent content, String keyword) {
        return keyword == null
                || (hasText(content.title())
                        && content.title().toLowerCase(Locale.ROOT)
                                .contains(keyword.toLowerCase(Locale.ROOT)));
    }

    private boolean hasCoordinate(TourApiContent content) {
        return content.latitude() != null && content.longitude() != null;
    }

    private String normalizeName(String name) {
        return name == null
                ? ""
                : NAME_SEPARATOR.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private int distanceBetween(PlaceCoordinate from, BigDecimal latitude, BigDecimal longitude) {
        return distanceBetween(from.latitude(), from.longitude(), latitude, longitude);
    }

    private int distanceBetween(
            BigDecimal fromLatitude,
            BigDecimal fromLongitude,
            BigDecimal toLatitude,
            BigDecimal toLongitude) {
        double startLatitude = Math.toRadians(fromLatitude.doubleValue());
        double endLatitude = Math.toRadians(toLatitude.doubleValue());
        double latitudeDelta = endLatitude - startLatitude;
        double longitudeDelta = Math.toRadians(toLongitude.doubleValue() - fromLongitude.doubleValue());
        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(startLatitude)
                        * Math.cos(endLatitude)
                        * Math.pow(Math.sin(longitudeDelta / 2), 2);
        double centralAngle = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return (int) Math.round(EARTH_RADIUS_METERS * centralAngle);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private BusinessException invalidRequest() {
        return new BusinessException(ErrorCode.INVALID_COURSE_PLACE_CANDIDATE_REQUEST);
    }

    private static PlaceCoordinate coordinate(String latitude, String longitude) {
        return new PlaceCoordinate(new BigDecimal(latitude), new BigDecimal(longitude));
    }
}
