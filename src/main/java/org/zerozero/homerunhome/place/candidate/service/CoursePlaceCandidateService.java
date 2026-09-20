package org.zerozero.homerunhome.place.candidate.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
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
    private static final int MAX_COURSE_POINT_COUNT = 10;
    private static final int MAX_DISTANCE_METERS = 20_000;
    private static final int KAKAO_PAGE_SIZE = 15;
    private static final int TOUR_API_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final PlaceCoordinate STADIUM = coordinate("36.317067", "127.429188");
    private static final PlaceCoordinate DAEJEON_STATION = coordinate("36.3322", "127.4345");

    private final KakaoApiClient kakaoApiClient;
    private final KoreanTourApiClient koreanTourApiClient;
    private final CoursePlaceCandidateAssembler candidateAssembler;

    public CoursePlaceCandidateResponse findCandidates(CoursePlaceCandidateRequest request) {
        validateRequired(request);
        List<CoursePlaceCategory> categories = request.categories().stream().distinct().toList();
        String keyword = normalizeKeyword(request.keyword());
        List<PlaceCoordinate> centers = resolveCenters(request);
        PlaceCoordinate center = centers.isEmpty() ? null : centers.getFirst();
        int maxDistanceMeters = request.maxDistanceMeters() == null
                ? defaultDistance(request.scope(), request.hotspot())
                : request.maxDistanceMeters();
        int limit = request.limit() == null ? DEFAULT_LIMIT : request.limit();
        validateOptions(request, categories, centers, maxDistanceMeters, limit, request.eventDate());

        int perSearchLimit = Math.max(
                KAKAO_PAGE_SIZE,
                (int) Math.ceil((double) limit / (categories.size() * centers.size())));
        List<RankedCoursePlaceCandidate> candidates = new ArrayList<>();
        for (CoursePlaceCategory category : categories) {
            if (category == CoursePlaceCategory.FESTIVAL_EVENT) {
                candidates.addAll(findEventCandidates(
                        keyword, centers, maxDistanceMeters, request.eventDate()));
                continue;
            }
            for (PlaceCoordinate searchCenter : centers) {
                if (category.usesKakao()) {
                    candidates.addAll(findKakaoCandidates(
                            category,
                            keyword,
                            searchCenter,
                            maxDistanceMeters,
                            perSearchLimit));
                }
                if (category.usesNearbyTourApi()) {
                    candidates.addAll(findTourCandidates(
                            category, keyword, searchCenter, maxDistanceMeters));
                }
            }
        }

        List<CoursePlaceCandidate> places = candidateAssembler.mergeAndSort(candidates, limit);
        return new CoursePlaceCandidateResponse(
                request.scope(), center, centers, maxDistanceMeters, places);
    }

    private List<RankedCoursePlaceCandidate> findKakaoCandidates(
            CoursePlaceCategory category,
            String keyword,
            PlaceCoordinate center,
            int maxDistanceMeters,
            int resultLimit) {
        KakaoCoordinate kakaoCenter = toKakaoCoordinate(center);
        String query = keyword == null ? category.defaultKeyword() : keyword;
        List<KakaoPlace> places = new ArrayList<>();
        int page = 1;
        boolean end = false;
        while (places.size() < resultLimit && page <= 45 && !end) {
            var result = query == null
                    ? kakaoApiClient.searchPlacesByCategory(
                            KakaoCategoryPlaceSearchRequest.around(
                                    category.kakaoCode(),
                                    kakaoCenter,
                                    maxDistanceMeters,
                                    page,
                                    KAKAO_PAGE_SIZE))
                    : kakaoApiClient.searchPlaces(new KakaoPlaceSearchRequest(
                            query,
                            kakaoCenter,
                            maxDistanceMeters,
                            category.kakaoCode(),
                            null,
                            KakaoPlaceSearchSort.DISTANCE,
                            page,
                            KAKAO_PAGE_SIZE));
            places.addAll(result.items());
            end = result.end();
            page++;
        }
        List<RankedCoursePlaceCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < places.size() && candidates.size() < resultLimit; index++) {
            KakaoPlace place = places.get(index);
            if (place.coordinate() == null) {
                continue;
            }
            CoursePlaceCandidate candidate = candidateAssembler.from(place, category, center);
            if (candidate.distanceMeters() <= maxDistanceMeters) {
                candidates.add(new RankedCoursePlaceCandidate(candidate, index));
            }
        }
        return candidates;
    }

    private List<RankedCoursePlaceCandidate> findTourCandidates(
            CoursePlaceCategory category,
            String keyword,
            PlaceCoordinate center,
            int maxDistanceMeters) {
        List<TourApiContent> contents = koreanTourApiClient
                .findNearbyContents(
                        category.tourismContentType().tourApiId(),
                        center.longitude(),
                        center.latitude(),
                        maxDistanceMeters,
                        1,
                        TOUR_API_PAGE_SIZE)
                .items();
        List<RankedCoursePlaceCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < contents.size(); index++) {
            TourApiContent content = contents.get(index);
            if (!hasCoordinate(content) || !matchesKeyword(content, keyword)) {
                continue;
            }
            CoursePlaceCandidate candidate = candidateAssembler.from(content, category, center);
            if (candidate.distanceMeters() <= maxDistanceMeters) {
                candidates.add(new RankedCoursePlaceCandidate(candidate, index));
            }
        }
        return candidates;
    }

    private List<RankedCoursePlaceCandidate> findEventCandidates(
            String keyword,
            List<PlaceCoordinate> centers,
            int maxDistanceMeters,
            LocalDate eventDate) {
        List<TourApiContent> contents = koreanTourApiClient
                .findDaejeonEvents(eventDate, eventDate, 1, TOUR_API_PAGE_SIZE)
                .items();
        List<RankedCoursePlaceCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < contents.size(); index++) {
            TourApiContent content = contents.get(index);
            if (!hasCoordinate(content) || !matchesKeyword(content, keyword)) {
                continue;
            }
            CoursePlaceCandidate candidate = candidateAssembler.from(
                    content,
                    CoursePlaceCategory.FESTIVAL_EVENT,
                    candidateAssembler.nearestCenter(
                            centers, content.latitude(), content.longitude()));
            if (candidate.distanceMeters() <= maxDistanceMeters) {
                candidates.add(new RankedCoursePlaceCandidate(candidate, index));
            }
        }
        return candidates;
    }

    private List<PlaceCoordinate> resolveCenters(CoursePlaceCandidateRequest request) {
        return switch (request.scope()) {
            case STADIUM_NEARBY -> List.of(STADIUM);
            case STATION_NEARBY -> List.of(DAEJEON_STATION);
            case DAEJEON_HOTSPOT -> nullableCoordinate(hotspotCoordinate(request.hotspot()));
            case CURRENT_LOCATION, CUSTOM -> nullableCoordinate(request.center());
            case COURSE_NEARBY -> request.coursePoints() == null || request.coursePoints().isEmpty()
                    ? nullableCoordinate(request.center())
                    : request.coursePoints().stream().distinct().toList();
        };
    }

    private List<PlaceCoordinate> nullableCoordinate(PlaceCoordinate coordinate) {
        return coordinate == null ? List.of() : List.of(coordinate);
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
            CoursePlaceCandidateRequest request,
            List<CoursePlaceCategory> categories,
            List<PlaceCoordinate> centers,
            int maxDistanceMeters,
            int limit,
            LocalDate eventDate) {
        if (categories.size() > CoursePlaceCategory.values().length
                || centers.isEmpty()
                || centers.size() > MAX_COURSE_POINT_COUNT
                || centers.stream().anyMatch(center -> !validCoordinate(center))
                || maxDistanceMeters < 1
                || maxDistanceMeters > MAX_DISTANCE_METERS
                || limit < 1
                || limit > MAX_LIMIT
                || (request.scope() != PlaceExploreScope.COURSE_NEARBY
                        && request.coursePoints() != null
                        && !request.coursePoints().isEmpty())
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
