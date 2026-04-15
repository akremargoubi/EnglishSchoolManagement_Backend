package com.englishschool.attendance.config;  // Change for schedule-service

import com.netflix.discovery.shared.transport.jersey.TransportClientFactories;
import com.netflix.discovery.shared.transport.jersey3.Jersey3TransportClientFactories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EurekaConfig {

    @Bean
    public TransportClientFactories<?> transportClientFactories() {
        return new Jersey3TransportClientFactories();
    }
}
