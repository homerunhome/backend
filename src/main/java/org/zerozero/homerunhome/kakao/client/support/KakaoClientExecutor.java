package org.zerozero.homerunhome.kakao.client.support;

import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;

@Component
public class KakaoClientExecutor {

    private static final Logger log = LoggerFactory.getLogger(KakaoClientExecutor.class);

    private final String restApiKey;

    public KakaoClientExecutor(@Value("${kakao.rest-api-key:}") String restApiKey) {
        this.restApiKey = restApiKey;
    }

    public <T> T execute(String operation, Supplier<T> request) {
        if (!StringUtils.hasText(restApiKey)) {
            throw new BusinessException(ErrorCode.KAKAO_API_KEY_NOT_CONFIGURED);
        }

        try {
            return request.get();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            HttpStatusCode status = exception.getStatusCode();
            log.warn("Kakao API request failed: operation={}, httpStatus={}", operation, status.value());
            throw new BusinessException(errorCodeFor(status));
        } catch (RuntimeException exception) {
            log.warn(
                    "Kakao API request failed: operation={}, failureType={}, rootCauseType={}",
                    operation,
                    exception.getClass().getSimpleName(),
                    rootCause(exception).getClass().getSimpleName());
            throw new BusinessException(ErrorCode.KAKAO_API_REQUEST_FAILED);
        }
    }

    private ErrorCode errorCodeFor(HttpStatusCode status) {
        if (status.value() == 401 || status.value() == 403) {
            return ErrorCode.KAKAO_AUTHENTICATION_FAILED;
        }
        if (status.value() == 429) {
            return ErrorCode.KAKAO_QUOTA_EXCEEDED;
        }
        if (status.is5xxServerError()) {
            return ErrorCode.KAKAO_EXTERNAL_SERVER_ERROR;
        }
        return ErrorCode.KAKAO_API_REQUEST_FAILED;
    }

    private Throwable rootCause(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
