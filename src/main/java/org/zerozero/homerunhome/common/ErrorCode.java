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
    TRAIN_SCHEDULE_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "열차 시간표를 조회하지 못했습니다."),
    KAKAO_API_KEY_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "카카오 REST API 키가 설정되지 않았습니다."),
    KAKAO_AUTHENTICATION_FAILED(HttpStatus.BAD_GATEWAY, "카카오 API 인증에 실패했습니다."),
    KAKAO_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "카카오 API 호출 한도를 초과했습니다."),
    KAKAO_EXTERNAL_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "카카오 외부 서버에서 오류가 발생했습니다."),
    KAKAO_API_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "카카오 API 요청을 처리하지 못했습니다.");

    private final HttpStatus status;
    private final String message;
}
