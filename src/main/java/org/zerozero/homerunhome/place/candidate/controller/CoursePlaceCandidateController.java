package org.zerozero.homerunhome.place.candidate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCandidateRequest;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCandidateResponse;
import org.zerozero.homerunhome.place.candidate.service.CoursePlaceCandidateService;

@RestController
@RequestMapping("/api/course-place-candidates")
@RequiredArgsConstructor
@Tag(name = "코스 장소 후보", description = "카카오·TourAPI 통합 코스 장소 후보 조회")
public class CoursePlaceCandidateController {

    private final CoursePlaceCandidateService coursePlaceCandidateService;

    @PostMapping
    @Operation(summary = "위치 범위와 카테고리별 코스 장소 후보 조회")
    public CoursePlaceCandidateResponse findCandidates(
            @RequestBody CoursePlaceCandidateRequest request) {
        return coursePlaceCandidateService.findCandidates(request);
    }
}
