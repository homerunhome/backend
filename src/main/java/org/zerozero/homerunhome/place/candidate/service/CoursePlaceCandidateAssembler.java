package org.zerozero.homerunhome.place.candidate.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.zerozero.homerunhome.kakao.dto.response.KakaoPlace;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCandidate;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCategory;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceProvider;
import org.zerozero.homerunhome.place.explore.dto.PlaceCoordinate;
import org.zerozero.homerunhome.tourapi.dto.TourApiContent;

@Component
public class CoursePlaceCandidateAssembler {

    private static final int DUPLICATE_DISTANCE_METERS = 100;
    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private static final Pattern NAME_SEPARATOR = Pattern.compile("[\\p{Punct}\\s]+");
    private static final DateTimeFormatter API_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    CoursePlaceCandidate from(
            KakaoPlace place, CoursePlaceCategory category, PlaceCoordinate center) {
        return new CoursePlaceCandidate(
                CoursePlaceProvider.KAKAO,
                place.id(),
                place.name(),
                category,
                hasText(place.roadAddressName()) ? place.roadAddressName() : place.addressName(),
                place.phone(),
                place.coordinate().latitude(),
                place.coordinate().longitude(),
                null,
                place.placeUrl(),
                null,
                null,
                distance(center, place.coordinate().latitude(), place.coordinate().longitude()));
    }

    CoursePlaceCandidate from(
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
                content.phone(),
                content.latitude(),
                content.longitude(),
                hasText(content.imageUrl()) ? content.imageUrl() : content.thumbnailUrl(),
                "/api/tourism/contents/" + content.contentId(),
                parseDate(content.eventStartDate()),
                parseDate(content.eventEndDate()),
                distance(center, content.latitude(), content.longitude()));
    }

    List<CoursePlaceCandidate> mergeAndSort(
            List<RankedCoursePlaceCandidate> candidates, int limit) {
        List<RankedCoursePlaceCandidate> unique = new ArrayList<>();
        for (RankedCoursePlaceCandidate candidate : candidates) {
            int duplicateIndex = findDuplicateIndex(unique, candidate);
            if (duplicateIndex < 0) {
                unique.add(candidate);
            } else if (prefer(candidate, unique.get(duplicateIndex))) {
                unique.set(duplicateIndex, candidate);
            }
        }
        return unique.stream()
                .sorted(Comparator.comparingInt(candidate -> candidate.place().distanceMeters()))
                .limit(limit)
                .map(RankedCoursePlaceCandidate::place)
                .toList();
    }

    PlaceCoordinate nearestCenter(
            List<PlaceCoordinate> centers, BigDecimal latitude, BigDecimal longitude) {
        return centers.stream()
                .min(Comparator.comparingInt(center -> distance(center, latitude, longitude)))
                .orElseThrow();
    }

    private int findDuplicateIndex(
            List<RankedCoursePlaceCandidate> unique, RankedCoursePlaceCandidate candidate) {
        String candidateName = normalizeName(candidate.place().name());
        if (candidateName.isEmpty()) {
            return -1;
        }
        for (int index = 0; index < unique.size(); index++) {
            CoursePlaceCandidate existing = unique.get(index).place();
            if (candidateName.equals(normalizeName(existing.name()))
                    && distance(
                                    candidate.place().latitude(),
                                    candidate.place().longitude(),
                                    existing.latitude(),
                                    existing.longitude())
                            <= DUPLICATE_DISTANCE_METERS) {
                return index;
            }
        }
        return -1;
    }

    private boolean prefer(
            RankedCoursePlaceCandidate candidate, RankedCoursePlaceCandidate existing) {
        boolean candidateHasImage = hasText(candidate.place().imageUrl());
        boolean existingHasImage = hasText(existing.place().imageUrl());
        if (candidateHasImage != existingHasImage) {
            return candidateHasImage;
        }
        if (candidate.sourceRank() != existing.sourceRank()) {
            return candidate.sourceRank() < existing.sourceRank();
        }
        return candidate.place().distanceMeters() < existing.place().distanceMeters();
    }

    private LocalDate parseDate(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value, API_DATE);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String normalizeName(String name) {
        return name == null
                ? ""
                : NAME_SEPARATOR.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private int distance(PlaceCoordinate from, BigDecimal latitude, BigDecimal longitude) {
        return distance(from.latitude(), from.longitude(), latitude, longitude);
    }

    private int distance(
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
}
