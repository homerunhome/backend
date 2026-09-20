package org.zerozero.homerunhome.itinerary.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.itinerary.dto.ItineraryPlaceRecommendationRequest;
import org.zerozero.homerunhome.itinerary.dto.ItineraryResponse;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCandidateRequest;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCandidateResponse;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCategory;
import org.zerozero.homerunhome.place.candidate.service.CoursePlaceCandidateService;
import org.zerozero.homerunhome.place.explore.dto.PlaceCoordinate;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreScope;

@Service
@RequiredArgsConstructor
public class ItineraryPlaceRecommendationService {

    private static final int MAX_SEARCH_POINTS = 10;

    private final ItineraryService itineraryService;
    private final CoursePlaceCandidateService coursePlaceCandidateService;

    public CoursePlaceCandidateResponse recommend(
            Long itineraryId, ItineraryPlaceRecommendationRequest request) {
        if (request == null || request.categories() == null || request.categories().isEmpty()) {
            throw invalidRequest();
        }

        ItineraryResponse itinerary = itineraryService.get(itineraryId);
        List<PlaceCoordinate> coursePoints = itinerary.places().stream()
                .filter(place -> place.latitude() != null && place.longitude() != null)
                .map(place -> new PlaceCoordinate(place.latitude(), place.longitude()))
                .distinct()
                .toList();
        if (coursePoints.isEmpty()) {
            throw invalidRequest();
        }

        List<PlaceCoordinate> searchPoints = sampleSearchPoints(coursePoints);
        CoursePlaceCandidateRequest candidateRequest = new CoursePlaceCandidateRequest(
                PlaceExploreScope.COURSE_NEARBY,
                request.categories(),
                null,
                searchPoints,
                null,
                null,
                request.maxDistanceMeters(),
                request.categories().contains(CoursePlaceCategory.FESTIVAL_EVENT)
                        ? itinerary.gameDate()
                        : null,
                request.limit());
        return coursePlaceCandidateService.findCandidates(candidateRequest);
    }

    private List<PlaceCoordinate> sampleSearchPoints(List<PlaceCoordinate> coursePoints) {
        if (coursePoints.size() <= MAX_SEARCH_POINTS) {
            return coursePoints;
        }
        return java.util.stream.IntStream.range(0, MAX_SEARCH_POINTS)
                .map(index -> (int) Math.round(
                        (double) index * (coursePoints.size() - 1) / (MAX_SEARCH_POINTS - 1)))
                .mapToObj(coursePoints::get)
                .toList();
    }

    private BusinessException invalidRequest() {
        return new BusinessException(ErrorCode.INVALID_COURSE_PLACE_CANDIDATE_REQUEST);
    }
}
