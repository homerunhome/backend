package org.zerozero.homerunhome.kakao.client.local;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.kakao.client.support.KakaoClientExecutor;
import org.zerozero.homerunhome.kakao.dto.common.KakaoCoordinate;
import org.zerozero.homerunhome.kakao.dto.common.KakaoSearchResult;
import org.zerozero.homerunhome.kakao.dto.request.KakaoAddressSearchRequest;
import org.zerozero.homerunhome.kakao.dto.request.KakaoCategoryPlaceSearchRequest;
import org.zerozero.homerunhome.kakao.dto.request.KakaoPlaceSearchRequest;
import org.zerozero.homerunhome.kakao.dto.response.KakaoAddress;
import org.zerozero.homerunhome.kakao.dto.response.KakaoPlace;

@Component
public class KakaoLocalClient {

    private static final ParameterizedTypeReference<KakaoLocalResponse<KakaoLocalResponse.AddressDocument>>
            ADDRESS_RESPONSE_TYPE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<KakaoLocalResponse<KakaoLocalResponse.PlaceDocument>>
            PLACE_RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final KakaoClientExecutor executor;

    public KakaoLocalClient(
            @Qualifier("kakaoLocalRestClient") RestClient restClient,
            KakaoClientExecutor executor) {
        this.restClient = restClient;
        this.executor = executor;
    }

    public KakaoSearchResult<KakaoAddress> searchAddresses(KakaoAddressSearchRequest request) {
        return executor.execute("addressSearch", () -> {
            var response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v2/local/search/address.json")
                            .queryParam("query", request.query())
                            .queryParam("page", request.page())
                            .queryParam("size", request.size())
                            .build())
                    .retrieve()
                    .body(ADDRESS_RESPONSE_TYPE);
            return mapAddresses(response);
        });
    }

    public KakaoSearchResult<KakaoPlace> searchPlaces(KakaoPlaceSearchRequest request) {
        return executor.execute("placeSearch", () -> {
            var response = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/v2/local/search/keyword.json")
                                .queryParam("query", request.query())
                                .queryParam("sort", request.sort().apiValue())
                                .queryParam("page", request.page())
                                .queryParam("size", request.size());
                        if (request.center() != null) {
                            builder.queryParam("x", request.center().longitude().toPlainString())
                                    .queryParam("y", request.center().latitude().toPlainString());
                        }
                        if (request.radiusMeters() != null) {
                            builder.queryParam("radius", request.radiusMeters());
                        }
                        if (request.categoryGroupCode() != null) {
                            builder.queryParam("category_group_code", request.categoryGroupCode());
                        }
                        if (request.bounds() != null) {
                            builder.queryParam("rect", request.bounds().apiValue());
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(PLACE_RESPONSE_TYPE);
            return mapPlaces(response);
        });
    }

    public KakaoSearchResult<KakaoPlace> searchPlacesByCategory(KakaoCategoryPlaceSearchRequest request) {
        return executor.execute("categoryPlaceSearch", () -> {
            var response = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/v2/local/search/category.json")
                                .queryParam("category_group_code", request.categoryGroupCode())
                                .queryParam("page", request.page())
                                .queryParam("size", request.size());
                        if (request.center() != null) {
                            builder.queryParam("x", request.center().longitude().toPlainString())
                                    .queryParam("y", request.center().latitude().toPlainString())
                                    .queryParam("radius", request.radiusMeters())
                                    .queryParam("sort", "distance");
                        }
                        if (request.bounds() != null) {
                            builder.queryParam("rect", request.bounds().apiValue());
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(PLACE_RESPONSE_TYPE);
            return mapPlaces(response);
        });
    }

    private KakaoSearchResult<KakaoAddress> mapAddresses(
            KakaoLocalResponse<KakaoLocalResponse.AddressDocument> response) {
        requireValid(response);
        List<KakaoAddress> items = response.documents().stream()
                .map(document -> new KakaoAddress(
                        value(document.addressName()),
                        document.roadAddress() == null ? "" : value(document.roadAddress().addressName()),
                        coordinate(document.x(), document.y())))
                .toList();
        return result(response.meta(), items);
    }

    private KakaoSearchResult<KakaoPlace> mapPlaces(
            KakaoLocalResponse<KakaoLocalResponse.PlaceDocument> response) {
        requireValid(response);
        List<KakaoPlace> items = response.documents().stream()
                .map(document -> new KakaoPlace(
                        value(document.id()),
                        value(document.placeName()),
                        value(document.categoryName()),
                        value(document.categoryGroupCode()),
                        value(document.phone()),
                        value(document.addressName()),
                        value(document.roadAddressName()),
                        coordinate(document.x(), document.y()),
                        value(document.placeUrl()),
                        optionalInteger(document.distance())))
                .toList();
        return result(response.meta(), items);
    }

    private <T> KakaoSearchResult<T> result(KakaoLocalResponse.Meta meta, List<T> items) {
        return new KakaoSearchResult<>(items, meta.totalCount(), meta.pageableCount(), meta.end());
    }

    private void requireValid(KakaoLocalResponse<?> response) {
        if (response == null || response.meta() == null || response.documents() == null) {
            throw new BusinessException(ErrorCode.KAKAO_API_REQUEST_FAILED);
        }
    }

    private KakaoCoordinate coordinate(String longitude, String latitude) {
        return new KakaoCoordinate(new BigDecimal(longitude), new BigDecimal(latitude));
    }

    private Integer optionalInteger(String value) {
        return StringUtils.hasText(value) ? Integer.valueOf(value) : null;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
