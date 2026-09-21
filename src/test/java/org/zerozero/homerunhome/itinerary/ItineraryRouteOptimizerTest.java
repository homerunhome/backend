package org.zerozero.homerunhome.itinerary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.itinerary.dto.ItineraryAutoPlanRequest;
import org.zerozero.homerunhome.itinerary.service.ItineraryRouteOptimizer;

class ItineraryRouteOptimizerTest {

    private final ItineraryRouteOptimizer optimizer = new ItineraryRouteOptimizer();

    @Test
    void findsShortestRouteAcrossAllSelectedPlaces() {
        ItineraryRouteOptimizer.OptimizedRoute route = optimizer.optimize(
                List.of(place("a"), place("b")),
                List.of(
                        leg("ARRIVAL", "a", 1),
                        leg("ARRIVAL", "b", 5),
                        leg("a", "b", 100),
                        leg("b", "a", 1),
                        leg("a", "STADIUM", 1),
                        leg("b", "STADIUM", 1)));

        assertEquals(List.of("b", "a"), route.placeIds());
        assertEquals(List.of(5L, 1L), route.travelMinutes());
        assertEquals(1L, route.finalLegMinutes());
    }

    @Test
    void findsRouteWhenUnusedLegsHaveNoRoute() {
        ItineraryRouteOptimizer.OptimizedRoute route = optimizer.optimize(
                List.of(place("a"), place("b")),
                List.of(
                        leg("ARRIVAL", "a", 1),
                        leg("a", "b", 2),
                        leg("b", "STADIUM", 3)));

        assertEquals(List.of("a", "b"), route.placeIds());
        assertEquals(List.of(1L, 2L), route.travelMinutes());
        assertEquals(3L, route.finalLegMinutes());
    }

    @Test
    void rejectsWhenNoCompleteRouteExists() {
        BusinessException exception = assertThrows(BusinessException.class, () -> optimizer.optimize(
                List.of(place("a")), List.of(leg("ARRIVAL", "a", 1))));

        assertEquals(ErrorCode.ITINERARY_ROUTE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void rejectsWhenDirectRouteToStadiumIsMissing() {
        BusinessException exception = assertThrows(BusinessException.class, () -> optimizer.optimize(
                List.of(), List.of()));

        assertEquals(ErrorCode.ITINERARY_ROUTE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void usesCandidateInputOrderWhenRoutesHaveEqualDuration() {
        ItineraryRouteOptimizer.OptimizedRoute route = optimizer.optimize(
                List.of(place("a"), place("b")),
                List.of(
                        leg("ARRIVAL", "a", 1),
                        leg("ARRIVAL", "b", 1),
                        leg("a", "b", 1),
                        leg("b", "a", 1),
                        leg("a", "STADIUM", 1),
                        leg("b", "STADIUM", 1)));

        assertEquals(List.of("a", "b"), route.placeIds());
    }

    private ItineraryAutoPlanRequest.PlaceCandidate place(String id) {
        return new ItineraryAutoPlanRequest.PlaceCandidate(id, id, null, null, null, 30L, false);
    }

    private ItineraryAutoPlanRequest.RouteTime leg(String from, String to, long minutes) {
        return new ItineraryAutoPlanRequest.RouteTime(from, to, minutes);
    }
}
