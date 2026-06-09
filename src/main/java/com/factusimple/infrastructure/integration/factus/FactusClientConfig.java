package com.factusimple.infrastructure.integration.factus;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class FactusClientConfig {

    /** RestClient dedicado a Factus, con base URL y timeouts del proveedor. */
    @Bean("factusRestClient")
    public RestClient factusRestClient(FactusProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(properties.connectTimeout())
                .withReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .baseUrl(properties.endpoint())
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }
}
