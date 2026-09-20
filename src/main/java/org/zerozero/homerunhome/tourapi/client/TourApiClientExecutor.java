package org.zerozero.homerunhome.tourapi.client;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;

@Component
public class TourApiClientExecutor {

    private static final Logger log = LoggerFactory.getLogger(TourApiClientExecutor.class);

    private final String serviceKey;
    private final String mobileApp;
    private final TourApiResponseParser responseParser;

    public TourApiClientExecutor(
            @Value("${tour-api.service-key:}") String serviceKey,
            @Value("${tour-api.mobile-app:HomerunHome}") String mobileApp,
            TourApiResponseParser responseParser) {
        this.serviceKey = serviceKey;
        this.mobileApp = mobileApp;
        this.responseParser = responseParser;
    }

    public TourApiPage execute(
            RestClient restClient, String operation, Map<String, ?> parameters, int page, int size) {
        if (!StringUtils.hasText(serviceKey)) {
            throw new BusinessException(ErrorCode.TOUR_API_KEY_NOT_CONFIGURED);
        }

        try {
            String responseBody = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/" + operation)
                                .queryParam("serviceKey", serviceKey)
                                .queryParam("MobileOS", "ETC")
                                .queryParam("MobileApp", mobileApp)
                                .queryParam("_type", "json")
                                .queryParam("pageNo", page)
                                .queryParam("numOfRows", size);
                        parameters.forEach((name, value) -> {
                            if (value != null) {
                                builder.queryParam(name, value);
                            }
                        });
                        return builder.build();
                    })
                    .retrieve()
                    .body(String.class);
            return responseParser.parse(operation, responseBody, page, size);
        } catch (RuntimeException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            ErrorCode errorCode = mapHttpFailure(exception);
            logFailure(operation, exception);
            throw new BusinessException(errorCode);
        }
    }

    private ErrorCode mapHttpFailure(RuntimeException exception) {
        if (!(exception instanceof RestClientResponseException responseException)) {
            return ErrorCode.TOUR_API_EXTERNAL_SERVER_ERROR;
        }
        HttpStatusCode status = responseException.getStatusCode();
        if (status.value() == 401 || status.value() == 403) {
            return ErrorCode.TOUR_API_AUTHENTICATION_FAILED;
        }
        if (status.value() == 429) {
            return ErrorCode.TOUR_API_QUOTA_EXCEEDED;
        }
        return status.is5xxServerError()
                ? ErrorCode.TOUR_API_EXTERNAL_SERVER_ERROR
                : ErrorCode.TOUR_API_INVALID_REQUEST;
    }

    private void logFailure(String operation, RuntimeException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            log.warn(
                    "TourAPI request failed: operation={}, httpStatus={}",
                    operation,
                    responseException.getStatusCode().value());
            return;
        }
        log.warn(
                "TourAPI request failed: operation={}, failureType={}",
                operation,
                exception.getClass().getSimpleName());
    }
}
