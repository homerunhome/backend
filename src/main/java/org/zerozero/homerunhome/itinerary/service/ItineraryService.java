package org.zerozero.homerunhome.itinerary.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.game.domain.Game;
import org.zerozero.homerunhome.game.repository.GameRepository;
import org.zerozero.homerunhome.itinerary.domain.Itinerary;
import org.zerozero.homerunhome.itinerary.domain.ItineraryPlace;
import org.zerozero.homerunhome.itinerary.domain.ItineraryTimelineEntry;
import org.zerozero.homerunhome.itinerary.dto.ItineraryPlanRequest;
import org.zerozero.homerunhome.itinerary.dto.ItineraryRequest;
import org.zerozero.homerunhome.itinerary.dto.ItineraryResponse;
import org.zerozero.homerunhome.itinerary.repository.ItineraryRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItineraryService {

    private static final int DEFAULT_EXPECTED_GAME_DURATION_MINUTES = 180;
    private static final int OVERTIME_RISK_BUFFER_MINUTES = 60;
    private static final int TIGHT_ENTRY_SLACK_MINUTES = 15;
    private static final int ADEQUATE_ENTRY_SLACK_MINUTES = 60;

    private final ItineraryRepository itineraryRepository;
    private final GameRepository gameRepository;

    @Transactional
    public ItineraryResponse create(ItineraryRequest request) {
        Game game = gameRepository.findById(request.gameId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));
        Itinerary itinerary = Itinerary.builder()
                .game(game)
                .arrivalPlace(request.arrivalPlace())
                .arrivalAt(request.arrivalAt())
                .departurePlace(request.departurePlace())
                .departureAt(request.departureAt())
                .preferences(preferences(request))
                .places(places(request))
                .travelMode(request.travelMode())
                .stadiumEntryBufferMinutes(request.stadiumEntryBufferMinutes())
                .stadiumArrivalAt(request.stadiumArrivalAt())
                .gameStartSlackMinutes(request.gameStartSlackMinutes())
                .planStatus(request.status())
                .planWarnings(warnings(request))
                .timeline(timeline(request))
                .build();
        return ItineraryResponse.from(itineraryRepository.save(itinerary));
    }

    @Transactional
    public ItineraryResponse generate(ItineraryPlanRequest request) {
        Game game = gameRepository.findById(request.gameId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));
        if (!request.departureAt().isAfter(request.arrivalAt())) {
            throw new BusinessException(ErrorCode.INVALID_ITINERARY_PLAN_REQUEST);
        }

        LocalDateTime gameStartAt = LocalDateTime.of(game.getGameDate(), game.getGameStartTime());
        LocalDateTime expectedGameEndAt = gameStartAt.plusMinutes(
                request.expectedGameDurationMinutes() == null
                        ? DEFAULT_EXPECTED_GAME_DURATION_MINUTES
                        : request.expectedGameDurationMinutes());
        LocalDateTime stadiumDepartureRecommendedAt = request.departureAt()
                .minusMinutes(request.stadiumToDepartureMinutes())
                .minusMinutes(request.postGameCrowdBufferMinutes())
                .minusMinutes(request.boardingBufferMinutes());

        List<ItineraryPlace> places = new ArrayList<>();
        List<ItineraryTimelineEntry> timeline = new ArrayList<>();
        LocalDateTime cursor = request.arrivalAt();
        List<ItineraryPlanRequest.PlacePlanRequest> requestedPlaces = request.places() == null
                ? List.of()
                : request.places();
        for (ItineraryPlanRequest.PlacePlanRequest place : requestedPlaces) {
            LocalDateTime arrivesAt = cursor.plusMinutes(place.travelFromPreviousMinutes());
            LocalDateTime departsAt = arrivesAt.plusMinutes(place.stayDurationMinutes());
            timeline.add(new ItineraryTimelineEntry(
                    "PLACE_TRANSFER", place.name(), cursor, arrivesAt));
            timeline.add(new ItineraryTimelineEntry(
                    "PLACE_VISIT", place.name(), arrivesAt, departsAt));
            places.add(new ItineraryPlace(
                    place.placeId(),
                    place.name(),
                    place.address(),
                    place.latitude(),
                    place.longitude(),
                    arrivesAt,
                    departsAt,
                    place.stayDurationMinutes(),
                    place.mustVisit(),
                    true));
            cursor = departsAt;
        }

        LocalDateTime stadiumArrivalAt = cursor.plusMinutes(request.finalLegToStadiumMinutes());
        timeline.add(new ItineraryTimelineEntry(
                "STADIUM_TRANSFER", game.getStadium(), cursor, stadiumArrivalAt));
        timeline.add(new ItineraryTimelineEntry(
                "STADIUM_ARRIVAL", game.getStadium(), stadiumArrivalAt, stadiumArrivalAt));
        timeline.add(new ItineraryTimelineEntry(
                "EXPECTED_GAME", "예상 경기 시간", gameStartAt, expectedGameEndAt));
        timeline.add(new ItineraryTimelineEntry(
                "RETURN_PLAN", request.departurePlace(), stadiumDepartureRecommendedAt, request.departureAt()));
        timeline.sort(Comparator.comparing(ItineraryTimelineEntry::getStartsAt));

        long gameStartSlackMinutes = Duration.between(stadiumArrivalAt, gameStartAt).toMinutes();
        long entrySlackMinutes = gameStartSlackMinutes - request.stadiumEntryBufferMinutes();
        String status = planStatus(entrySlackMinutes, stadiumDepartureRecommendedAt, expectedGameEndAt);
        List<String> warnings = planWarnings(entrySlackMinutes, stadiumDepartureRecommendedAt, expectedGameEndAt);

        Itinerary itinerary = Itinerary.builder()
                .game(game)
                .arrivalPlace(request.arrivalPlace())
                .arrivalAt(request.arrivalAt())
                .departurePlace(request.departurePlace())
                .departureAt(request.departureAt())
                .preferences(List.of())
                .places(places)
                .stadiumEntryBufferMinutes(request.stadiumEntryBufferMinutes())
                .stadiumArrivalAt(stadiumArrivalAt)
                .gameStartSlackMinutes(gameStartSlackMinutes)
                .stadiumDepartureRecommendedAt(stadiumDepartureRecommendedAt)
                .expectedGameEndAt(expectedGameEndAt)
                .gameEndMayBeDelayed(true)
                .planStatus(status)
                .planWarnings(warnings)
                .timeline(timeline)
                .build();
        return ItineraryResponse.from(itineraryRepository.save(itinerary));
    }

    @Transactional
    public ItineraryResponse update(Long id, ItineraryRequest request) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITINERARY_NOT_FOUND));
        Game game = gameRepository.findById(request.gameId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));
        itinerary.update(
                game,
                request.arrivalPlace(),
                request.arrivalAt(),
                request.departurePlace(),
                request.departureAt(),
                preferences(request),
                places(request),
                request.travelMode(),
                request.stadiumEntryBufferMinutes(),
                request.stadiumArrivalAt(),
                request.gameStartSlackMinutes(),
                request.status(),
                warnings(request),
                timeline(request));
        return ItineraryResponse.from(itinerary);
    }

    public ItineraryResponse get(Long id) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITINERARY_NOT_FOUND));
        return ItineraryResponse.from(itinerary);
    }

    private List<String> preferences(ItineraryRequest request) {
        return request.preferences() == null ? List.of() : request.preferences();
    }

    private List<ItineraryPlace> places(ItineraryRequest request) {
        return request.places() == null
                ? List.of()
                : request.places().stream()
                        .map(place -> new ItineraryPlace(
                                place.placeId(),
                                place.name(),
                                place.address(),
                                place.latitude(),
                                place.longitude(),
                                place.arrivesAt(),
                                place.departsAt(),
                                place.stayDurationMinutes(),
                                place.mustVisit(),
                                place.scheduled()))
                        .toList();
    }

    private List<String> warnings(ItineraryRequest request) {
        return request.warnings() == null ? List.of() : request.warnings();
    }

    private List<ItineraryTimelineEntry> timeline(ItineraryRequest request) {
        return request.timeline() == null
                ? List.of()
                : request.timeline().stream()
                        .map(entry -> new ItineraryTimelineEntry(
                                entry.type(), entry.description(), entry.startsAt(), entry.endsAt()))
                        .toList();
    }

    private String planStatus(
            long entrySlackMinutes,
            LocalDateTime stadiumDepartureRecommendedAt,
            LocalDateTime expectedGameEndAt) {
        if (expectedGameEndAt.plusMinutes(OVERTIME_RISK_BUFFER_MINUTES)
                .isAfter(stadiumDepartureRecommendedAt)) {
            return "RETURN_CAUTION";
        }
        if (entrySlackMinutes < 0) {
            return "GAME_LATE_POSSIBLE";
        }
        if (entrySlackMinutes < TIGHT_ENTRY_SLACK_MINUTES) {
            return "TIGHT";
        }
        if (entrySlackMinutes < ADEQUATE_ENTRY_SLACK_MINUTES) {
            return "ADEQUATE";
        }
        return "AMPLE";
    }

    private List<String> planWarnings(
            long entrySlackMinutes,
            LocalDateTime stadiumDepartureRecommendedAt,
            LocalDateTime expectedGameEndAt) {
        List<String> warnings = new ArrayList<>();
        warnings.add("예상 종료 이후 연장전 등으로 종료가 늦어질 수 있습니다.");
        if (entrySlackMinutes < 0) {
            warnings.add("입장 여유시간 부족: 경기 지각 가능");
        }
        if (expectedGameEndAt.plusMinutes(OVERTIME_RISK_BUFFER_MINUTES)
                .isAfter(stadiumDepartureRecommendedAt)) {
            warnings.add("연장전·경기 지연 시 귀가편 탑승이 어려울 수 있습니다.");
        }
        return warnings;
    }
}
