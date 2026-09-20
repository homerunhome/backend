package org.zerozero.homerunhome.kakao.client.local;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record KakaoLocalResponse<T>(Meta meta, List<T> documents) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Meta(
            @JsonProperty("total_count") int totalCount,
            @JsonProperty("pageable_count") int pageableCount,
            @JsonProperty("is_end") boolean end) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AddressDocument(
            @JsonProperty("address_name") String addressName,
            String x,
            String y,
            @JsonProperty("road_address") AddressDetails roadAddress) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AddressDetails(@JsonProperty("address_name") String addressName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlaceDocument(
            String id,
            @JsonProperty("place_name") String placeName,
            @JsonProperty("category_name") String categoryName,
            @JsonProperty("category_group_code") String categoryGroupCode,
            String phone,
            @JsonProperty("address_name") String addressName,
            @JsonProperty("road_address_name") String roadAddressName,
            String x,
            String y,
            @JsonProperty("place_url") String placeUrl,
            String distance) {}
}
