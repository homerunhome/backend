package org.zerozero.homerunhome.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 본문 또는 날짜·시간 형식이 올바르지 않습니다."),
    ITINERARY_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
