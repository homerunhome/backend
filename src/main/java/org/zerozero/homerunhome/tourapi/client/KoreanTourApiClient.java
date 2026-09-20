package org.zerozero.homerunhome.tourapi.client;

import static org.zerozero.homerunhome.tourapi.client.TourApiFieldReader.decimal;
import static org.zerozero.homerunhome.tourapi.client.TourApiFieldReader.text;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.zerozero.homerunhome.tourapi.dto.TourApiContent;
import org.zerozero.homerunhome.tourapi.dto.TourApiImage;
import org.zerozero.homerunhome.tourapi.dto.TourApiIntro;

@Component
public class KoreanTourApiClient {

    private static final String DAEJEON_AREA_CODE = "3";
    private static final DateTimeFormatter API_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestClient restClient;
    private final TourApiClientExecutor executor;

    public KoreanTourApiClient(
            @Qualifier("koreanTourRestClient") RestClient restClient,
            TourApiClientExecutor executor) {
        this.restClient = restClient;
        this.executor = executor;
    }

    public TourApiPageResult<TourApiContent> findDaejeonContents(
            String contentTypeId, int page, int size) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("areaCode", DAEJEON_AREA_CODE);
        parameters.put("contentTypeId", contentTypeId);
        parameters.put("arrange", "A");
        TourApiPage response = executor.execute(restClient, "areaBasedList2", parameters, page, size);
        return mapPage(response, this::toContent);
    }

    public TourApiPageResult<TourApiContent> findNearbyContents(
            String contentTypeId,
            BigDecimal longitude,
            BigDecimal latitude,
            int radiusMeters,
            int page,
            int size) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("mapX", longitude);
        parameters.put("mapY", latitude);
        parameters.put("radius", radiusMeters);
        parameters.put("contentTypeId", contentTypeId);
        parameters.put("arrange", "E");
        TourApiPage response = executor.execute(restClient, "locationBasedList2", parameters, page, size);
        return mapPage(response, this::toContent);
    }

    public Optional<TourApiContent> findDetail(String contentId) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("contentId", contentId);
        parameters.put("defaultYN", "Y");
        parameters.put("firstImageYN", "Y");
        parameters.put("areacodeYN", "Y");
        parameters.put("catcodeYN", "Y");
        parameters.put("addrinfoYN", "Y");
        parameters.put("mapinfoYN", "Y");
        parameters.put("overviewYN", "Y");
        TourApiPage response = executor.execute(restClient, "detailCommon2", parameters, 1, 1);
        return response.items().stream().findFirst().map(this::toContent);
    }

    public List<TourApiImage> findImages(String contentId) {
        Map<String, Object> parameters = Map.of(
                "contentId", contentId,
                "imageYN", "Y",
                "subImageYN", "Y");
        return executor.execute(restClient, "detailImage2", parameters, 1, 100).items().stream()
                .map(this::toImage)
                .toList();
    }

    public Optional<TourApiIntro> findIntro(String contentId, String contentTypeId) {
        Map<String, Object> parameters = Map.of(
                "contentId", contentId,
                "contentTypeId", contentTypeId);
        TourApiPage response = executor.execute(restClient, "detailIntro2", parameters, 1, 1);
        return response.items().stream().findFirst().map(this::toIntro);
    }

    public TourApiPageResult<TourApiContent> findDaejeonEvents(
            LocalDate startDate, LocalDate endDate, int page, int size) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("eventStartDate", startDate.format(API_DATE));
        parameters.put("eventEndDate", endDate.format(API_DATE));
        parameters.put("areaCode", DAEJEON_AREA_CODE);
        parameters.put("arrange", "A");
        TourApiPage response = executor.execute(restClient, "searchFestival2", parameters, page, size);
        return mapPage(response, this::toContent);
    }

    private TourApiContent toContent(Map<String, Object> fields) {
        return new TourApiContent(
                text(fields, "contentid"),
                text(fields, "contenttypeid"),
                text(fields, "title"),
                text(fields, "addr1"),
                text(fields, "addr2"),
                text(fields, "tel"),
                text(fields, "homepage"),
                text(fields, "overview"),
                text(fields, "firstimage"),
                text(fields, "firstimage2"),
                decimal(fields, "mapx"),
                decimal(fields, "mapy"),
                text(fields, "eventstartdate"),
                text(fields, "eventenddate"));
    }

    private TourApiImage toImage(Map<String, Object> fields) {
        return new TourApiImage(
                text(fields, "imgname"),
                text(fields, "originimgurl"),
                text(fields, "smallimageurl"),
                text(fields, "cpyrhtDivCd"));
    }

    private TourApiIntro toIntro(Map<String, Object> fields) {
        return new TourApiIntro(
                firstText(fields, "usetime", "usetimeculture", "playtime"),
                firstText(fields, "restdate", "restdateculture"),
                firstText(fields, "usefee", "usetimefestival"),
                firstText(fields, "parking", "parkingculture", "parkingfee"),
                firstText(fields, "spendtime", "spendtimefestival"),
                text(fields, "eventplace"),
                text(fields, "bookingplace"));
    }

    private String firstText(Map<String, Object> fields, String... names) {
        for (String name : names) {
            String value = text(fields, name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private <T> TourApiPageResult<T> mapPage(
            TourApiPage response, java.util.function.Function<Map<String, Object>, T> mapper) {
        return new TourApiPageResult<>(
                response.items().stream().map(mapper).toList(),
                response.page(),
                response.size(),
                response.totalCount());
    }
}
