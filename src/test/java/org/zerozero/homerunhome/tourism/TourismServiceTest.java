package org.zerozero.homerunhome.tourism;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.tourapi.client.BarrierFreeTourApiClient;
import org.zerozero.homerunhome.tourapi.client.KoreanTourApiClient;
import org.zerozero.homerunhome.tourapi.client.TourApiPageResult;
import org.zerozero.homerunhome.tourapi.dto.TourApiAccessibility;
import org.zerozero.homerunhome.tourapi.dto.TourApiContent;
import org.zerozero.homerunhome.tourapi.dto.TourApiImage;
import org.zerozero.homerunhome.tourapi.dto.TourApiIntro;
import org.zerozero.homerunhome.tourism.dto.TourismContentType;
import org.zerozero.homerunhome.tourism.service.TourismContentService;
import org.zerozero.homerunhome.tourism.service.TourismEventService;

class TourismServiceTest {

    private KoreanTourApiClient koreanClient;
    private BarrierFreeTourApiClient barrierFreeClient;
    private TourismContentService contentService;
    private TourismEventService eventService;

    @BeforeEach
    void setUp() {
        koreanClient = mock(KoreanTourApiClient.class);
        barrierFreeClient = mock(BarrierFreeTourApiClient.class);
        contentService = new TourismContentService(koreanClient, barrierFreeClient);
        eventService = new TourismEventService(koreanClient);
    }

    @Test
    void returnsPagedDaejeonTourismContents() {
        when(koreanClient.findDaejeonContents("12", 1, 12))
                .thenReturn(new TourApiPageResult<>(List.of(content()), 1, 12, 13));

        var response = contentService.findDaejeonContents(
                TourismContentType.TOURIST_ATTRACTION, 1, 12);

        assertEquals(TourismContentType.TOURIST_ATTRACTION, response.contentType());
        assertThat(response.contents()).singleElement()
                .extracting(item -> item.title())
                .isEqualTo("한밭수목원");
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void combinesDetailImagesAndBarrierFreeInformation() {
        when(koreanClient.findDetail("125407")).thenReturn(Optional.of(content()));
        when(koreanClient.findImages("125407")).thenReturn(List.of(new TourApiImage(
                "전경", "https://example.com/original.jpg", "https://example.com/small.jpg", "Type1")));
        when(koreanClient.findIntro("125407", "12")).thenReturn(Optional.of(new TourApiIntro(
                "09:00~18:00", "매주 월요일", "무료", "주차 가능", "1시간", "", "")));
        when(barrierFreeClient.findDetail("125407")).thenReturn(Optional.of(accessibility()));

        var response = contentService.findDetail("125407");

        assertEquals("도심 속 수목원 & 휴식 공간", response.overview());
        assertEquals("https://example.com", response.homepageUrl());
        assertThat(response.images()).singleElement()
                .extracting(image -> image.originalUrl())
                .isEqualTo("https://example.com/original.jpg");
        assertEquals("09:00~18:00", response.usageInformation().useTime());
        assertEquals("매주 월요일", response.usageInformation().restDate());
        assertEquals("대여 가능", response.accessibility().wheelchair());
    }

    @Test
    void allowsContentWithoutImagesOrBarrierFreeInformation() {
        when(koreanClient.findDetail("125407")).thenReturn(Optional.of(content()));
        when(koreanClient.findImages("125407")).thenReturn(List.of());
        when(koreanClient.findIntro("125407", "12")).thenReturn(Optional.empty());
        when(barrierFreeClient.findDetail("125407")).thenReturn(Optional.empty());

        var response = contentService.findDetail("125407");

        assertThat(response.images()).isEmpty();
        assertThat(response.usageInformation()).isNull();
        assertThat(response.accessibility()).isNull();
    }

    @Test
    void distinguishesMissingContentFromInvalidContentId() {
        when(koreanClient.findDetail("999999")).thenReturn(Optional.empty());

        BusinessException notFound = assertThrows(
                BusinessException.class, () -> contentService.findDetail("999999"));
        BusinessException invalid = assertThrows(
                BusinessException.class, () -> contentService.findDetail("invalid"));

        assertEquals(ErrorCode.TOURISM_CONTENT_NOT_FOUND, notFound.getErrorCode());
        assertEquals(ErrorCode.INVALID_TOURISM_REQUEST, invalid.getErrorCode());
    }

    @Test
    void returnsEventsForGameDateRange() {
        LocalDate startDate = LocalDate.of(2026, 9, 20);
        LocalDate endDate = LocalDate.of(2026, 9, 21);
        TourApiContent event = new TourApiContent(
                "event-1",
                "15",
                "대전 축제",
                "대전 중구",
                "",
                "042-123-4567",
                "",
                "",
                "https://example.com/event.jpg",
                "",
                new BigDecimal("127.43"),
                new BigDecimal("36.33"),
                "20260920",
                "20260921");
        when(koreanClient.findDaejeonEvents(startDate, endDate, 1, 12))
                .thenReturn(new TourApiPageResult<>(List.of(event), 1, 12, 1));

        var response = eventService.findDaejeonEvents(startDate, endDate, 1, 12);

        assertThat(response.events()).singleElement().satisfies(result -> {
            assertEquals(startDate, result.startDate());
            assertEquals(endDate, result.endDate());
            assertEquals("대전 축제", result.title());
        });
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void rejectsReversedOrExcessiveEventDateRange() {
        LocalDate date = LocalDate.of(2026, 9, 20);

        BusinessException reversed = assertThrows(
                BusinessException.class,
                () -> eventService.findDaejeonEvents(date, date.minusDays(1), 1, 12));
        BusinessException excessive = assertThrows(
                BusinessException.class,
                () -> eventService.findDaejeonEvents(date, date.plusDays(32), 1, 12));

        assertEquals(ErrorCode.INVALID_TOURISM_REQUEST, reversed.getErrorCode());
        assertEquals(ErrorCode.INVALID_TOURISM_REQUEST, excessive.getErrorCode());
    }

    @Test
    void rejectsUnsupportedContentTypeAndInvalidPagination() {
        BusinessException contentType = assertThrows(
                BusinessException.class,
                () -> contentService.findDaejeonContents(TourismContentType.UNKNOWN, 1, 12));
        BusinessException pagination = assertThrows(
                BusinessException.class,
                () -> contentService.findDaejeonContents(TourismContentType.TOURIST_ATTRACTION, 0, 12));

        assertEquals(ErrorCode.INVALID_TOURISM_REQUEST, contentType.getErrorCode());
        assertEquals(ErrorCode.INVALID_TOURISM_REQUEST, pagination.getErrorCode());
    }

    private TourApiContent content() {
        return new TourApiContent(
                "125407",
                "12",
                "한밭수목원",
                "대전 서구 둔산대로 169",
                "",
                "042-123-4567",
                "<a href=\"https://example.com\">홈페이지</a>",
                "<b>도심 속 수목원</b> &amp; 휴식 공간",
                "https://example.com/image.jpg",
                "https://example.com/thumb.jpg",
                new BigDecimal("127.388"),
                new BigDecimal("36.366"),
                "",
                "");
    }

    private TourApiAccessibility accessibility() {
        return new TourApiAccessibility(
                "장애인 주차구역 있음",
                "",
                "",
                "",
                "대여 가능",
                "",
                "",
                "장애인 화장실 있음",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "");
    }
}
