package com.englishschool.gatewayservice.config;

// CORS is handled exclusively via application.properties globalcors.
// Do NOT add a CorsWebFilter bean here — it causes duplicate
// Access-Control-Allow-Origin headers which browsers reject.
public class CorsConfig {
    // intentionally empty
}