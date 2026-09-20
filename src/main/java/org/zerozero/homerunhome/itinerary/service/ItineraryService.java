package org.zerozero.homerunhome.itinerary.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
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

    @Transactional
    public ItineraryResponse create(ItineraryRequest request) {
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
                .gameId(request.gameId())
                .gameDate(request.gameDate())
                .gameStartTime(request.gameStartTime())
                .stadium(request.stadium())
                .homeTeam(request.homeTeam())
                .awayTeam(request.awayTeam())
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
