package org.zerozero.homerunhome.itinerary.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.itinerary.dto.ItineraryAutoPlanRequest;

@Service
public class ItineraryRouteOptimizer {

    private static final int MAX_PLACE_COUNT = 8;

    public OptimizedRoute optimize(
            List<ItineraryAutoPlanRequest.PlaceCandidate> places,
            List<ItineraryAutoPlanRequest.RouteTime> routeTimes) {
        List<ItineraryAutoPlanRequest.PlaceCandidate> candidates = places == null ? List.of() : places;
        if (candidates.size() > MAX_PLACE_COUNT || routeTimes == null) {
            throw invalidPlan();
        }

        Set<String> placeIds = new HashSet<>();
        for (ItineraryAutoPlanRequest.PlaceCandidate place : candidates) {
            if (place == null
                    || place.placeId() == null
                    || place.placeId().isBlank()
                    || ItineraryAutoPlanRequest.ARRIVAL_NODE_ID.equals(place.placeId())
                    || ItineraryAutoPlanRequest.STADIUM_NODE_ID.equals(place.placeId())
                    || !placeIds.add(place.placeId())) {
                throw invalidPlan();
            }
        }

        List<String> orderedInputIds = candidates.stream()
                .map(ItineraryAutoPlanRequest.PlaceCandidate::placeId)
                .toList();
        Map<RouteEdge, Long> minutesByEdge = routeMatrix(orderedInputIds, routeTimes);
        int count = orderedInputIds.size();
        if (count == 0) {
            Long directRouteMinutes = minutesByEdge.get(edge(
                    ItineraryAutoPlanRequest.ARRIVAL_NODE_ID, ItineraryAutoPlanRequest.STADIUM_NODE_ID));
            if (directRouteMinutes == null) {
                throw noCompleteRoute();
            }
            return new OptimizedRoute(List.of(), List.of(), directRouteMinutes);
        }

        int stateCount = 1 << count;
        long[][] bestMinutes = new long[stateCount][count];
        int[][] previousPlace = new int[stateCount][count];
        for (int mask = 0; mask < stateCount; mask++) {
            java.util.Arrays.fill(bestMinutes[mask], Long.MAX_VALUE);
            java.util.Arrays.fill(previousPlace[mask], -1);
        }

        for (int place = 0; place < count; place++) {
            Long arrivalMinutes = minutesByEdge.get(edge(
                    ItineraryAutoPlanRequest.ARRIVAL_NODE_ID, orderedInputIds.get(place)));
            if (arrivalMinutes != null) {
                bestMinutes[1 << place][place] = arrivalMinutes;
            }
        }

        for (int mask = 1; mask < stateCount; mask++) {
            for (int last = 0; last < count; last++) {
                long currentMinutes = bestMinutes[mask][last];
                if ((mask & (1 << last)) == 0 || currentMinutes == Long.MAX_VALUE) {
                    continue;
                }
                for (int next = 0; next < count; next++) {
                    if ((mask & (1 << next)) != 0) {
                        continue;
                    }
                    Long legMinutes = minutesByEdge.get(edge(orderedInputIds.get(last), orderedInputIds.get(next)));
                    if (legMinutes == null) {
                        continue;
                    }
                    int nextMask = mask | (1 << next);
                    long candidateMinutes = currentMinutes + legMinutes;
                    if (candidateMinutes < bestMinutes[nextMask][next]
                            || (candidateMinutes == bestMinutes[nextMask][next]
                                    && isEarlierPath(
                                            append(pathFor(previousPlace, mask, last), next),
                                            pathFor(previousPlace, nextMask, next)))) {
                        bestMinutes[nextMask][next] = candidateMinutes;
                        previousPlace[nextMask][next] = last;
                    }
                }
            }
        }

        int completeMask = stateCount - 1;
        int lastPlace = -1;
        long bestTotalMinutes = Long.MAX_VALUE;
        for (int place = 0; place < count; place++) {
            Long finalLegMinutes = minutesByEdge.get(
                    edge(orderedInputIds.get(place), ItineraryAutoPlanRequest.STADIUM_NODE_ID));
            if (bestMinutes[completeMask][place] == Long.MAX_VALUE || finalLegMinutes == null) {
                continue;
            }
            long totalMinutes = bestMinutes[completeMask][place] + finalLegMinutes;
            if (totalMinutes < bestTotalMinutes
                    || (totalMinutes == bestTotalMinutes
                            && (lastPlace < 0 || isEarlierPath(
                                    pathFor(previousPlace, completeMask, place),
                                    pathFor(previousPlace, completeMask, lastPlace))))) {
                bestTotalMinutes = totalMinutes;
                lastPlace = place;
            }
        }
        if (lastPlace < 0) {
            throw noCompleteRoute();
        }

        int[] order = new int[count];
        int mask = completeMask;
        int place = lastPlace;
        for (int index = count - 1; index >= 0; index--) {
            order[index] = place;
            int previous = previousPlace[mask][place];
            mask ^= 1 << place;
            place = previous;
        }

        List<String> orderedPlaceIds = new ArrayList<>(count);
        List<Long> travelMinutes = new ArrayList<>(count);
        String previousId = ItineraryAutoPlanRequest.ARRIVAL_NODE_ID;
        for (int index : order) {
            String currentId = orderedInputIds.get(index);
            orderedPlaceIds.add(currentId);
            travelMinutes.add(minutesByEdge.get(edge(previousId, currentId)));
            previousId = currentId;
        }
        long finalLegMinutes = minutesByEdge.get(edge(previousId, ItineraryAutoPlanRequest.STADIUM_NODE_ID));
        return new OptimizedRoute(orderedPlaceIds, travelMinutes, finalLegMinutes);
    }

    private Map<RouteEdge, Long> routeMatrix(
            List<String> placeIds, List<ItineraryAutoPlanRequest.RouteTime> routeTimes) {
        Set<RouteEdge> expectedEdges = new HashSet<>();
        for (String placeId : placeIds) {
            expectedEdges.add(edge(ItineraryAutoPlanRequest.ARRIVAL_NODE_ID, placeId));
            expectedEdges.add(edge(placeId, ItineraryAutoPlanRequest.STADIUM_NODE_ID));
            for (String destinationId : placeIds) {
                if (!placeId.equals(destinationId)) {
                    expectedEdges.add(edge(placeId, destinationId));
                }
            }
        }
        if (placeIds.isEmpty()) {
            expectedEdges.add(edge(ItineraryAutoPlanRequest.ARRIVAL_NODE_ID, ItineraryAutoPlanRequest.STADIUM_NODE_ID));
        }

        Map<RouteEdge, Long> minutesByEdge = new HashMap<>();
        for (ItineraryAutoPlanRequest.RouteTime routeTime : routeTimes) {
            if (routeTime == null || routeTime.fromPlaceId() == null || routeTime.toPlaceId() == null
                    || routeTime.minutes() == null || routeTime.minutes() < 0) {
                throw invalidPlan();
            }
            RouteEdge routeEdge = edge(routeTime.fromPlaceId(), routeTime.toPlaceId());
            if (!expectedEdges.contains(routeEdge) || minutesByEdge.putIfAbsent(routeEdge, routeTime.minutes()) != null) {
                throw invalidPlan();
            }
        }
        return minutesByEdge;
    }

    private RouteEdge edge(String from, String to) {
        return new RouteEdge(from, to);
    }

    private int[] pathFor(int[][] previousPlace, int mask, int last) {
        int[] path = new int[Integer.bitCount(mask)];
        for (int index = path.length - 1; index >= 0; index--) {
            path[index] = last;
            int previous = previousPlace[mask][last];
            mask ^= 1 << last;
            last = previous;
        }
        return path;
    }

    private int[] append(int[] path, int place) {
        int[] result = java.util.Arrays.copyOf(path, path.length + 1);
        result[result.length - 1] = place;
        return result;
    }

    private boolean isEarlierPath(int[] candidate, int[] current) {
        for (int index = 0; index < candidate.length; index++) {
            if (candidate[index] != current[index]) {
                return candidate[index] < current[index];
            }
        }
        return false;
    }

    private BusinessException invalidPlan() {
        return new BusinessException(ErrorCode.INVALID_ITINERARY_PLAN_REQUEST);
    }

    private BusinessException noCompleteRoute() {
        return new BusinessException(ErrorCode.ITINERARY_ROUTE_NOT_FOUND);
    }

    private record RouteEdge(String from, String to) {}

    public record OptimizedRoute(List<String> placeIds, List<Long> travelMinutes, long finalLegMinutes) {}
}
