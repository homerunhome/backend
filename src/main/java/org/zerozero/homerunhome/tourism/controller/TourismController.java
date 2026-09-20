package org.zerozero.homerunhome.tourism.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zerozero.homerunhome.tourism.dto.TourismContentDetailResponse;
import org.zerozero.homerunhome.tourism.dto.TourismContentListResponse;
import org.zerozero.homerunhome.tourism.dto.TourismContentType;
import org.zerozero.homerunhome.tourism.dto.TourismEventListResponse;
import org.zerozero.homerunhome.tourism.service.TourismContentService;
import org.zerozero.homerunhome.tourism.service.TourismEventService;

@RestController
@RequestMapping("/api/tourism")
@RequiredArgsConstructor
@Tag(name = "관광정보", description = "TourAPI 기반 대전 관광 콘텐츠·행사·무장애 정보")
public class TourismController {

    private final TourismContentService tourismContentService;
    private final TourismEventService tourismEventService;

    @GetMapping("/contents")
    @Operation(summary = "대전 관광 콘텐츠 목록 조회")
    public TourismContentListResponse findContents(
            @RequestParam(defaultValue = "TOURIST_ATTRACTION") TourismContentType type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return tourismContentService.findDaejeonContents(type, page, size);
    }

    @GetMapping("/contents/{contentId}")
    @Operation(summary = "관광 콘텐츠 상세·사진·무장애 정보 조회")
    public TourismContentDetailResponse findDetail(
            @Parameter(description = "TourAPI 콘텐츠 ID", example = "125407")
                    @PathVariable String contentId) {
        return tourismContentService.findDetail(contentId);
    }

    @GetMapping("/events")
    @Operation(summary = "경기 날짜 전후 대전 행사 조회")
    public TourismEventListResponse findEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return tourismEventService.findDaejeonEvents(startDate, endDate, page, size);
    }
}
