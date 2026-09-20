package org.zerozero.homerunhome.kakao.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class KakaoConfiguration {

    @Bean
    @Qualifier("kakaoLocalRestClient")
    RestClient kakaoLocalRestClient(
            @Value("${kakao.local-base-url:https://dapi.kakao.com}") String baseUrl,
            @Value("${kakao.rest-api-key:}") String restApiKey,
            @Value("${kakao.connect-timeout:2s}") Duration connectTimeout,
            @Value("${kakao.read-timeout:3s}") Duration readTimeout) {
        return kakaoRestClient(baseUrl, restApiKey, connectTimeout, readTimeout);
    }

    @Bean
    @Qualifier("kakaoMobilityRestClient")
    RestClient kakaoMobilityRestClient(
            @Value("${kakao.mobility-base-url:https://apis-navi.kakaomobility.com}") String baseUrl,
            @Value("${kakao.rest-api-key:}") String restApiKey,
            @Value("${kakao.connect-timeout:2s}") Duration connectTimeout,
            @Value("${kakao.read-timeout:3s}") Duration readTimeout) {
        return kakaoRestClient(baseUrl, restApiKey, connectTimeout, readTimeout);
    }

    private RestClient kakaoRestClient(
            String baseUrl, String restApiKey, Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey)
                .build();
    }
}
