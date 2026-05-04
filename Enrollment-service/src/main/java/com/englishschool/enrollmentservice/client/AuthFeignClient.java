package com.englishschool.enrollmentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "auth-service", url = "${auth.service.url:http://localhost:1999/api}")
public interface AuthFeignClient {

    @PutMapping("/users/{id}/wallet/deduct")
    Map<String, Object> deductWallet(@PathVariable("id") String userId,
                                     @RequestBody Map<String, Double> body);
}
