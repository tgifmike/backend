package com.backend.backend.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
public class WeatherService {

    private final RestTemplate restTemplate;

    public WeatherService() {
        this.restTemplate = new RestTemplate();

        // Add headers interceptor
        restTemplate.setInterceptors(List.of((ClientHttpRequestInterceptor)
                (HttpRequest request, byte[] body, ClientHttpRequestExecution execution) -> {
                    request.getHeaders().add("User-Agent", "MyApp (contact@example.com)");
                    request.getHeaders().add("Accept", "application/ld+json");
                    return execution.execute(request, body);
                }));
    }

    @Cacheable(value = "weatherCache", key = "#lat + ',' + #lon")
    public Map<String, Object> getWeather(double lat, double lon) {
        // Step 1: Get metadata for the point
        String pointUrl = String.format("https://api.weather.gov/points/%f,%f", lat, lon);
        Map<String, Object> pointData = restTemplate.getForObject(pointUrl, Map.class);

        if (pointData == null || !pointData.containsKey("properties")) {
            throw new RuntimeException("Unable to fetch point data from weather.gov");
        }

        Map<String, Object> props = (Map<String, Object>) pointData.get("properties");

        String forecastUrl = (String) props.get("forecast");
        String hourlyUrl = (String) props.get("forecastHourly");

        // Step 2: Get forecast data
        Map<String, Object> forecast = restTemplate.getForObject(forecastUrl, Map.class);
        Map<String, Object> hourly = restTemplate.getForObject(hourlyUrl, Map.class);

        // Step 3: Get alerts for this point
        String alertsUrl = UriComponentsBuilder
                .fromHttpUrl("https://api.weather.gov/alerts/active")
                .queryParam("point", lat + "," + lon)
                .toUriString();

        Map<String, Object> alerts = restTemplate.getForObject(alertsUrl, Map.class);

        // Step 4: Combine all responses
        return Map.of(
                "forecast", forecast,
                "hourly", hourly,
                "alerts", alerts
        );
    }
}

