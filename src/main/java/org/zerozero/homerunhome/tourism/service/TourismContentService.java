package org.zerozero.homerunhome.tourism.service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.tourapi.client.BarrierFreeTourApiClient;
import org.zerozero.homerunhome.tourapi.client.KoreanTourApiClient;
import org.zerozero.homerunhome.tourapi.dto.TourApiAccessibility;
import org.zerozero.homerunhome.tourapi.dto.TourApiContent;
import org.zerozero.homerunhome.tourapi.dto.TourApiImage;
import org.zerozero.homerunhome.tourism.dto.TourismAccessibilityResponse;
import org.zerozero.homerunhome.tourism.dto.TourismContentDetailResponse;
import org.zerozero.homerunhome.tourism.dto.TourismContentListResponse;
import org.zerozero.homerunhome.tourism.dto.TourismContentSummaryResponse;
import org.zerozero.homerunhome.tourism.dto.TourismContentType;
import org.zerozero.homerunhome.tourism.dto.TourismImageResponse;

@Service
@RequiredArgsConstructor
public class TourismContentService {

    private static final int MAX_PAGE = 1000;
    private static final int MAX_SIZE = 100;
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern HREF = Pattern.compile("href=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    private final KoreanTourApiClient koreanTourApiClient;
    private final BarrierFreeTourApiClient barrierFreeTourApiClient;

    public TourismContentListResponse findDaejeonContents(
            TourismContentType contentType, int page, int size) {
        validateContentType(contentType);
        validatePage(page, size);
        var result = koreanTourApiClient.findDaejeonContents(contentType.tourApiId(), page, size);
        return new TourismContentListResponse(
                contentType,
                result.items().stream().map(this::toSummary).toList(),
                result.page(),
                result.size(),
                result.totalCount(),
                hasNext(result.page(), result.size(), result.totalCount()));
    }

    public TourismContentDetailResponse findDetail(String contentId) {
        validateContentId(contentId);
        TourApiContent content = koreanTourApiClient
                .findDetail(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOURISM_CONTENT_NOT_FOUND));
        List<TourismImageResponse> images = koreanTourApiClient.findImages(contentId).stream()
                .map(this::toImage)
                .toList();
        TourismAccessibilityResponse accessibility = barrierFreeTourApiClient
                .findDetail(contentId)
                .map(this::toAccessibility)
                .orElse(null);
        return new TourismContentDetailResponse(
                content.contentId(),
                TourismContentType.fromTourApiId(content.contentTypeId()),
                content.title(),
                plainText(content.overview()),
                content.address(),
                content.addressDetail(),
                content.latitude(),
                content.longitude(),
                content.phone(),
                homepageUrl(content.homepage()),
                content.imageUrl(),
                content.thumbnailUrl(),
                images,
                accessibility);
    }

    private TourismContentSummaryResponse toSummary(TourApiContent content) {
        return new TourismContentSummaryResponse(
                content.contentId(),
                TourismContentType.fromTourApiId(content.contentTypeId()),
                content.title(),
                content.address(),
                content.addressDetail(),
                content.latitude(),
                content.longitude(),
                content.phone(),
                content.imageUrl(),
                content.thumbnailUrl());
    }

    private TourismImageResponse toImage(TourApiImage image) {
        return new TourismImageResponse(
                image.name(), image.originalUrl(), image.thumbnailUrl(), image.copyrightType());
    }

    private TourismAccessibilityResponse toAccessibility(TourApiAccessibility info) {
        return new TourismAccessibilityResponse(
                info.parking(),
                info.route(),
                info.publicTransport(),
                info.ticketOffice(),
                info.wheelchair(),
                info.exit(),
                info.elevator(),
                info.restroom(),
                info.auditorium(),
                info.room(),
                info.disabilityEtc(),
                info.brailleBlock(),
                info.helpDog(),
                info.humanGuide(),
                info.audioGuide(),
                info.bigPrint(),
                info.braillePromotion(),
                info.guideSystem(),
                info.visualImpairmentEtc(),
                info.signGuide(),
                info.videoGuide(),
                info.hearingRoom(),
                info.hearingImpairmentEtc(),
                info.stroller(),
                info.lactationRoom(),
                info.babyChair(),
                info.infantsFamilyEtc());
    }

    private String plainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String withoutTags = HTML_TAG.matcher(html).replaceAll(" ");
        return WHITESPACE.matcher(HtmlUtils.htmlUnescape(withoutTags)).replaceAll(" ").trim();
    }

    private String homepageUrl(String homepage) {
        if (homepage == null || homepage.isBlank()) {
            return "";
        }
        Matcher matcher = HREF.matcher(homepage);
        return HtmlUtils.htmlUnescape(matcher.find() ? matcher.group(1) : plainText(homepage));
    }

    private void validateContentType(TourismContentType contentType) {
        if (contentType == null || !contentType.searchable()) {
            throw invalidRequest();
        }
    }

    private void validatePage(int page, int size) {
        if (page < 1 || page > MAX_PAGE || size < 1 || size > MAX_SIZE) {
            throw invalidRequest();
        }
    }

    private void validateContentId(String contentId) {
        if (contentId == null || !contentId.matches("[0-9]{1,20}")) {
            throw invalidRequest();
        }
    }

    private boolean hasNext(int page, int size, int totalCount) {
        return (long) page * size < totalCount;
    }

    private BusinessException invalidRequest() {
        return new BusinessException(ErrorCode.INVALID_TOURISM_REQUEST);
    }
}
