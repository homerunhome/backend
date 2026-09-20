package org.zerozero.homerunhome.tourism.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.tourapi.client.KoreanTourApiClient;
import org.zerozero.homerunhome.tourapi.dto.TourApiContent;
import org.zerozero.homerunhome.tourism.dto.TourismEventListResponse;
import org.zerozero.homerunhome.tourism.dto.TourismEventResponse;

@Service
@RequiredArgsConstructor
public class TourismEventService {

    private static final int MAX_PAGE = 1000;
    private static final int MAX_SIZE = 100;
    private static final int MAX_DATE_RANGE_DAYS = 31;
    private static final DateTimeFormatter API_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final KoreanTourApiClient koreanTourApiClient;

    public TourismEventListResponse findDaejeonEvents(
            LocalDate startDate, LocalDate endDate, int page, int size) {
        validate(startDate, endDate, page, size);
        var result = koreanTourApiClient.findDaejeonEvents(startDate, endDate, page, size);
        return new TourismEventListResponse(
                startDate,
                endDate,
                result.items().stream().map(this::toEvent).toList(),
                result.page(),
                result.size(),
                result.totalCount(),
                (long) result.page() * result.size() < result.totalCount());
    }

    private TourismEventResponse toEvent(TourApiContent content) {
        return new TourismEventResponse(
                content.contentId(),
                content.title(),
                parseDate(content.eventStartDate()),
                parseDate(content.eventEndDate()),
                content.address(),
                content.addressDetail(),
                content.latitude(),
                content.longitude(),
                content.phone(),
                content.imageUrl(),
                content.thumbnailUrl());
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value, API_DATE);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private void validate(LocalDate startDate, LocalDate endDate, int page, int size) {
        if (startDate == null
                || endDate == null
                || endDate.isBefore(startDate)
                || ChronoUnit.DAYS.between(startDate, endDate) > MAX_DATE_RANGE_DAYS
                || page < 1
                || page > MAX_PAGE
                || size < 1
                || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_TOURISM_REQUEST);
        }
    }
}
