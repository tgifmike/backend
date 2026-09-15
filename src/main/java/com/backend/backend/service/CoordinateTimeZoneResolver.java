package com.backend.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class CoordinateTimeZoneResolver {
    private final String endpoint;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public CoordinateTimeZoneResolver(
            @Value("${timezone.resolver-url:https://api-bdc.net/data/timezone-by-location}") String endpoint,
            @Value("${TIMEZONE_RESOLVER_API_KEY:}") String apiKey) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    public Optional<String> resolve(double latitude, double longitude) {
        try {
            String separator = endpoint.contains("?") ? "&" : "?";
            String keyQuery = apiKey == null || apiKey.isBlank() ? "" : "&key=" + apiKey;
            URI uri = URI.create(endpoint + separator + "latitude=" + latitude + "&longitude=" + longitude + keyQuery);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            InputStream responseStream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            if (responseCode != 200) {
                String error = responseStream == null ? "" : new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
                System.err.println("Time-zone resolver returned HTTP " + responseCode
                        + (error.isBlank() ? "" : ": " + error.substring(0, Math.min(error.length(), 500))));
                return Optional.empty();
            }
            JsonNode body = mapper.readTree(responseStream);
            String zone = body.path("ianaTimeId").asText(null);
            if (zone == null || zone.isBlank()) zone = body.path("timeZone").asText(null);
            if (zone == null || zone.isBlank()) return Optional.empty();
            ZoneId.of(zone);
            return Optional.of(zone);
        } catch (Exception exception) {
            System.err.println("Time-zone resolver failed: " + exception.getMessage());
            return Optional.empty();
        }
    }
}
