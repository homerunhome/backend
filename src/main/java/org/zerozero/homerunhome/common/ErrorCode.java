package org.zerozero.homerunhome.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 본문 또는 날짜·시간 형식이 올바르지 않습니다."),
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "선택한 경기를 찾을 수 없습니다."),
    ITINERARY_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."),
    TAGO_API_KEY_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "열차 정보 API 인증키가 설정되지 않았습니다."),
    TRAIN_SCHEDULE_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "열차 시간표를 조회하지 못했습니다.");

    private final HttpStatus status;
    private final String message;
}
