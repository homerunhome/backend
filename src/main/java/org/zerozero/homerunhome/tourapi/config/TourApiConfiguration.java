package org.zerozero.homerunhome.tourapi.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class TourApiConfiguration {

    @Bean
    @Qualifier("koreanTourRestClient")
    RestClient koreanTourRestClient(
            @Value("${tour-api.base-url:https://apis.data.go.kr/B551011}") String baseUrl,
            @Value("${tour-api.connect-timeout:2s}") Duration connectTimeout,
            @Value("${tour-api.read-timeout:5s}") Duration readTimeout) {
        return tourApiRestClient(baseUrl + "/KorService2", connectTimeout, readTimeout);
    }

    @Bean
    @Qualifier("barrierFreeTourRestClient")
    RestClient barrierFreeTourRestClient(
            @Value("${tour-api.base-url:https://apis.data.go.kr/B551011}") String baseUrl,
            @Value("${tour-api.connect-timeout:2s}") Duration connectTimeout,
            @Value("${tour-api.read-timeout:5s}") Duration readTimeout) {
        return tourApiRestClient(baseUrl + "/KorWithService2", connectTimeout, readTimeout);
    }

    private RestClient tourApiRestClient(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
