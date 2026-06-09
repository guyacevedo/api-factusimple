package com.factusimple;

import com.factusimple.infrastructure.config.AppProperties;
import com.factusimple.infrastructure.integration.factus.FactusProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableConfigurationProperties({AppProperties.class, FactusProperties.class})
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class ApiFactusimpleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiFactusimpleApplication.class, args);
    }
}
