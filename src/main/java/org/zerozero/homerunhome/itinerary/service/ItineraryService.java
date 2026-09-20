package org.zerozero.homerunhome.itinerary.service;

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
import org.zerozero.homerunhome.itinerary.dto.ItineraryRequest;
import org.zerozero.homerunhome.itinerary.dto.ItineraryResponse;
import org.zerozero.homerunhome.itinerary.repository.ItineraryRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItineraryService {

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
}
