package com.englishschool.enrollmentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "payment-service", url = "${payment.service.url:http://localhost:8083}")
public interface PaymentFeignClient {

    @PostMapping("/api/payments")
    Map<String, Object> createPayment(@RequestBody Map<String, Object> body);
}
