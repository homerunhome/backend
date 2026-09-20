package org.zerozero.homerunhome.train.client;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.train.dto.TrainCityResponse;
import org.zerozero.homerunhome.train.dto.TrainScheduleResponse;
import org.zerozero.homerunhome.train.dto.TrainStationResponse;

@Component
public class TagoTrainClient {

    private static final DateTimeFormatter TAGO_DATE_TIME_SECONDS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Logger log = LoggerFactory.getLogger(TagoTrainClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final String serviceKey;

    public TagoTrainClient(
            RestClient.Builder builder,
            @Value("${tago.base-url:}") String baseUrl,
            @Value("${tago.service-key:}") String serviceKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.serviceKey = serviceKey;
    }

    public List<TrainScheduleResponse> findKtxSchedules(
            String departureStationId, String arrivalStationId, LocalDate date) {
        return findSchedulesByGradeCode(departureStationId, arrivalStationId, date, "00").stream()
                .filter(schedule -> schedule.trainType().toUpperCase(Locale.ROOT).contains("KTX"))
                .toList();
    }

    public List<String> findTrainGradeCodes() {
        return parseItems(request("GetVhcleKndList", Map.of())).stream()
                .map(item -> stringValue(item.get("vehiclekndid")))
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    public List<TrainScheduleResponse> findSchedulesByGradeCode(
            String departureStationId, String arrivalStationId, LocalDate date, String gradeCode) {
        Map<String, Object> response = request(
                "GetStrtpntAlocFndTrainInfo",
                Map.of(
                        "depPlaceId", departureStationId,
                        "arrPlaceId", arrivalStationId,
                        "depPlandTime", date.format(DateTimeFormatter.BASIC_ISO_DATE),
                        "trainGradeCode", gradeCode));
        return parseSchedules(response);
    }

    public List<TrainCityResponse> findCities() {
        return parseItems(request("GetCtyCodeList", Map.of())).stream()
                .map(item -> new TrainCityResponse(
                        stringValue(item.get("citycode")), stringValue(item.get("cityname"))))
                .toList();
    }

    public List<TrainStationResponse> findStations(String cityCode) {
        return parseItems(request("GetCtyAcctoTrainSttnList", Map.of("cityCode", cityCode))).stream()
                .map(item -> new TrainStationResponse(
                        stringValue(item.get("nodeid")),
                        stringValue(item.get("nodename")),
                        stringValue(item.get("citycode"))))
                .toList();
    }

    private Map<String, Object> request(String operation, Map<String, String> parameters) {
        if (!StringUtils.hasText(serviceKey)) {
            throw new BusinessException(ErrorCode.TAGO_API_KEY_NOT_CONFIGURED);
        }

        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/" + operation)
                                .queryParam("serviceKey", serviceKey)
                                .queryParam("_type", "json")
                                .queryParam("numOfRows", 100)
                                .queryParam("pageNo", 1);
                        parameters.forEach(builder::queryParam);
                        return builder.build();
                    })
                    .retrieve()
                    .body(RESPONSE_TYPE);
            Map<?, ?> responseNode = childMap(response, "response");
            Map<?, ?> header = childMap(responseNode, "header");
            String resultCode = stringValue(header.get("resultCode"));
            if (!"00".equals(resultCode)) {
                log.warn(
                        "TAGO request returned an unsuccessful response: operation={}, resultCode={}",
                        operation,
                        safeProviderCode(resultCode));
                throw new BusinessException(ErrorCode.TRAIN_SCHEDULE_PROVIDER_ERROR);
            }
            return response;
        } catch (RuntimeException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            if (exception instanceof RestClientResponseException responseException) {
                log.warn(
                        "TAGO request failed: operation={}, httpStatus={}",
                        operation,
                        responseException.getStatusCode().value());
            } else {
                log.warn(
                        "TAGO request failed: operation={}, failureType={}, rootCauseType={}",
                        operation,
                        exception.getClass().getSimpleName(),
                        rootCause(exception).getClass().getSimpleName());
            }
            throw new BusinessException(ErrorCode.TRAIN_SCHEDULE_PROVIDER_ERROR);
        }
    }

    private String safeProviderCode(String resultCode) {
        return resultCode.matches("[A-Za-z0-9_-]{1,32}") ? resultCode : "unrecognized";
    }

    private Throwable rootCause(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private List<TrainScheduleResponse> parseSchedules(Map<String, Object> response) {
        List<TrainScheduleResponse> schedules = new ArrayList<>();
        for (Map<?, ?> fields : parseItems(response)) {
            String trainType = stringValue(fields.get("traingradename"));
            schedules.add(new TrainScheduleResponse(
                    stringValue(fields.get("trainno")),
                    trainType,
                    stringValue(fields.get("depplacename")),
                    parseDateTime(fields.get("depplandtime")),
                    stringValue(fields.get("arrplacename")),
                    parseDateTime(fields.get("arrplandtime"))));
        }
        return List.copyOf(schedules);
    }

    private List<Map<?, ?>> parseItems(Map<String, Object> response) {
        Map<?, ?> responseNode = childMap(response, "response");
        Map<?, ?> header = childMap(responseNode, "header");
        if (!"00".equals(stringValue(header.get("resultCode")))) {
            throw new BusinessException(ErrorCode.TRAIN_SCHEDULE_PROVIDER_ERROR);
        }
        Map<?, ?> body = childMap(responseNode, "body");
        Map<?, ?> items = childMap(body, "items");
        Object itemValue = items.get("item");
        if (itemValue == null) {
            return List.of();
        }
        List<?> itemList = itemValue instanceof List<?> list ? list : List.of(itemValue);
        List<Map<?, ?>> result = new ArrayList<>(itemList.size());
        for (Object item : itemList) {
            if (!(item instanceof Map<?, ?> fields)) {
                log.warn("TAGO response contained an item with an unexpected structure");
                throw new BusinessException(ErrorCode.TRAIN_SCHEDULE_PROVIDER_ERROR);
            }
            result.add(fields);
        }
        return List.copyOf(result);
    }

    private Map<?, ?> childMap(Object parent, String key) {
        if (parent instanceof Map<?, ?> map && map.get(key) instanceof Map<?, ?> child) {
            return child;
        }
        return Map.of();
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private LocalDateTime parseDateTime(Object value) {
        String dateTime = stringValue(value);
        try {
            return LocalDateTime.parse(dateTime, TAGO_DATE_TIME_SECONDS);
        } catch (DateTimeParseException exception) {
            log.warn("TAGO response contained an invalid schedule date-time field: valueLength={}", dateTime.length());
            throw new BusinessException(ErrorCode.TRAIN_SCHEDULE_PROVIDER_ERROR);
        }
    }
}
