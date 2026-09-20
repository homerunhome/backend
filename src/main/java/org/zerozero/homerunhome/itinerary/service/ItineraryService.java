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
        List<String> preferences = request.preferences() == null ? List.of() : request.preferences();
        List<ItineraryPlace> places = request.places() == null
                ? List.of()
                : request.places().stream()
                        .map(place -> new ItineraryPlace(
                                place.placeId(),
                                place.name(),
                                place.address(),
                                place.latitude(),
                                place.longitude()))
                        .toList();

        Itinerary itinerary = Itinerary.builder()
                .gameId(game.getGameId())
                .gameDate(game.getGameDate())
                .gameStartTime(game.getGameStartTime())
                .stadium(game.getStadium())
                .homeTeam(game.getHomeTeam())
                .awayTeam(game.getAwayTeam())
                .arrivalPlace(request.arrivalPlace())
                .arrivalAt(request.arrivalAt())
                .departurePlace(request.departurePlace())
                .departureAt(request.departureAt())
                .preferences(preferences)
                .places(places)
                .build();
        return ItineraryResponse.from(itineraryRepository.save(itinerary));
    }

    public ItineraryResponse get(Long id) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITINERARY_NOT_FOUND));
        return ItineraryResponse.from(itinerary);
    }
}
