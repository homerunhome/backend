package org.zerozero.homerunhome.geocoding.service;

import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.geocoding.dto.AddressCoordinateResponse;
import org.zerozero.homerunhome.kakao.client.KakaoApiClient;
import org.zerozero.homerunhome.kakao.dto.request.KakaoAddressSearchRequest;
import org.zerozero.homerunhome.kakao.dto.response.KakaoAddress;

@Service
@RequiredArgsConstructor
public class AddressGeocodingService {

    private static final int MAX_ADDRESS_LENGTH = 200;
    private static final int SEARCH_RESULT_SIZE = 30;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final KakaoApiClient kakaoApiClient;

    public AddressCoordinateResponse geocode(String address) {
        String query = validateAndNormalize(address);
        var result = kakaoApiClient.searchAddresses(new KakaoAddressSearchRequest(query, 1, SEARCH_RESULT_SIZE));
        KakaoAddress selected = selectAddress(query, result.items(), result.totalCount());
        return new AddressCoordinateResponse(
                selected.addressName(),
                selected.roadAddressName(),
                selected.coordinate().latitude(),
                selected.coordinate().longitude());
    }

    private String validateAndNormalize(String address) {
        if (address == null || address.isBlank()) {
            throw new BusinessException(ErrorCode.ADDRESS_REQUIRED);
        }
        String trimmed = address.trim();
        if (trimmed.length() > MAX_ADDRESS_LENGTH || trimmed.codePoints().anyMatch(Character::isISOControl)) {
            throw new BusinessException(ErrorCode.INVALID_ADDRESS);
        }
        return normalizeWhitespace(trimmed);
    }

    private KakaoAddress selectAddress(String query, List<KakaoAddress> addresses, int totalCount) {
        if (addresses.isEmpty()) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        List<KakaoAddress> exactMatches = addresses.stream()
                .filter(address -> matches(query, address.addressName())
                        || matches(query, address.roadAddressName()))
                .toList();
        if (exactMatches.size() == 1) {
            return exactMatches.getFirst();
        }
        if (exactMatches.size() > 1 || totalCount > 1 || addresses.size() > 1) {
            throw new BusinessException(ErrorCode.ADDRESS_AMBIGUOUS);
        }
        return addresses.getFirst();
    }

    private boolean matches(String query, String candidate) {
        return candidate != null && normalizeWhitespace(candidate.trim()).equals(query);
    }

    private String normalizeWhitespace(String value) {
        return WHITESPACE.matcher(value).replaceAll(" ");
    }
}
