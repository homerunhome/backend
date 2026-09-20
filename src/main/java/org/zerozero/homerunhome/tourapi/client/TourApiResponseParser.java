package org.zerozero.homerunhome.tourapi.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class TourApiResponseParser {

    private static final Logger log = LoggerFactory.getLogger(TourApiResponseParser.class);
    private static final TypeReference<Map<String, Object>> RESPONSE_TYPE = new TypeReference<>() {};
    private static final Pattern XML_RESULT_CODE =
            Pattern.compile("<resultCode>([^<]+)</resultCode>", Pattern.CASE_INSENSITIVE);
    private static final Pattern XML_RESULT_MESSAGE =
            Pattern.compile("<resultMsg>([^<]+)</resultMsg>", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;

    public TourApiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TourApiPage parse(
            String operation, String responseBody, int requestedPage, int requestedSize) {
        Map<String, Object> response = parseResponseBody(responseBody);
        Map<?, ?> responseNode = requiredChildMap(response, "response");
        Map<?, ?> header = requiredChildMap(responseNode, "header");
        String resultCode = text(header.get("resultCode"));
        if (!"0000".equals(resultCode) && !"00".equals(resultCode)) {
            log.warn(
                    "TourAPI request returned an unsuccessful response: operation={}, resultCode={}",
                    operation,
                    safeProviderCode(resultCode));
            throw providerFailure(resultCode, text(header.get("resultMsg")));
        }

        Map<?, ?> body = requiredChildMap(responseNode, "body");
        return new TourApiPage(
                extractItems(body.get("items")),
                integer(body.get("pageNo"), requestedPage),
                integer(body.get("numOfRows"), requestedSize),
                integer(body.get("totalCount"), 0));
    }

    private Map<String, Object> parseResponseBody(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new BusinessException(ErrorCode.TOUR_API_RESPONSE_INVALID);
        }
        String trimmed = responseBody.trim();
        if (trimmed.startsWith("<")) {
            String resultCode = xmlValue(XML_RESULT_CODE, trimmed);
            if (StringUtils.hasText(resultCode)) {
                throw providerFailure(resultCode, xmlValue(XML_RESULT_MESSAGE, trimmed));
            }
            throw new BusinessException(ErrorCode.TOUR_API_RESPONSE_INVALID);
        }
        try {
            return objectMapper.readValue(trimmed, RESPONSE_TYPE);
        } catch (JacksonException exception) {
            throw new BusinessException(ErrorCode.TOUR_API_RESPONSE_INVALID);
        }
    }

    private List<Map<String, Object>> extractItems(Object itemsValue) {
        if (!(itemsValue instanceof Map<?, ?> itemsNode)) {
            return List.of();
        }
        Object itemValue = itemsNode.get("item");
        if (itemValue == null || itemValue instanceof String string && string.isBlank()) {
            return List.of();
        }
        List<?> rawItems = itemValue instanceof List<?> list ? list : List.of(itemValue);
        List<Map<String, Object>> items = new ArrayList<>(rawItems.size());
        for (Object rawItem : rawItems) {
            if (!(rawItem instanceof Map<?, ?> fields)) {
                throw new BusinessException(ErrorCode.TOUR_API_RESPONSE_INVALID);
            }
            Map<String, Object> item = new LinkedHashMap<>();
            fields.forEach((key, value) -> item.put(String.valueOf(key), value));
            items.add(Collections.unmodifiableMap(item));
        }
        return List.copyOf(items);
    }

    private BusinessException providerFailure(String resultCode, String resultMessage) {
        String failure = (resultCode + " " + resultMessage).toUpperCase(Locale.ROOT);
        if (isOneOf(resultCode, "20", "30", "31")
                || failure.contains("SERVICE_KEY")
                || failure.contains("PERMISSION_DENIED")
                || failure.contains("ACCESS_DENIED")
                || failure.contains("DEADLINE_HAS_EXPIRED")) {
            return new BusinessException(ErrorCode.TOUR_API_AUTHENTICATION_FAILED);
        }
        if (isOneOf(resultCode, "22", "23") || failure.contains("LIMITED_NUMBER")) {
            return new BusinessException(ErrorCode.TOUR_API_QUOTA_EXCEEDED);
        }
        if ("10".equals(resultCode) || failure.contains("INVALID_REQUEST_PARAMETER")) {
            return new BusinessException(ErrorCode.TOUR_API_INVALID_REQUEST);
        }
        return new BusinessException(ErrorCode.TOUR_API_EXTERNAL_SERVER_ERROR);
    }

    private Map<?, ?> requiredChildMap(Object parent, String key) {
        if (parent instanceof Map<?, ?> map && map.get(key) instanceof Map<?, ?> child) {
            return child;
        }
        throw new BusinessException(ErrorCode.TOUR_API_RESPONSE_INVALID);
    }

    private boolean isOneOf(String value, String... candidates) {
        for (String candidate : candidates) {
            if (candidate.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private String xmlValue(Pattern pattern, String xml) {
        Matcher matcher = pattern.matcher(xml);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String safeProviderCode(String resultCode) {
        return resultCode.matches("[A-Za-z0-9_-]{1,64}") ? resultCode : "unrecognized";
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private int integer(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(text(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
