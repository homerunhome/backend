package org.zerozero.homerunhome.kakao.dto.response;

import org.zerozero.homerunhome.kakao.dto.common.KakaoCoordinate;

public record KakaoAddress(String addressName, String roadAddressName, KakaoCoordinate coordinate) {}
