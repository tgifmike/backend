package com.backend.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class GeocodingService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    public record GeocodeResult(double latitude, double longitude, boolean fromZipFallback) {}

    /**
     * Tries to geocode the full address first. If no result, falls back to ZIP code.
     * Returns lat/lon and a flag indicating if fallback was used.
     */
    public Optional<GeocodeResult> getLatLongFromAddressWithFallback(String fullAddress, String zipCode) {
        Optional<double[]> fullResult = getLatLongFromAddress(fullAddress);
        if (fullResult.isPresent()) {
            double[] coords = fullResult.get();
            return Optional.of(new GeocodeResult(coords[0], coords[1], false));
        }

        System.out.println("Falling back to ZIP code for geocoding: " + zipCode);
        Optional<double[]> zipResult = getLatLongFromAddress(zipCode);
        return zipResult.map(coords -> new GeocodeResult(coords[0], coords[1], true));
    }

    /**
     * Gets latitude and longitude for a given query using Nominatim.
     * Returns Optional.empty() if no result or error occurs.
     */
    public Optional<double[]> getLatLongFromAddress(String address) {
        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String requestUrl = NOMINATIM_URL + "?q=" + encodedAddress + "&format=json&limit=1";

            HttpURLConnection conn = (HttpURLConnection) new URL(requestUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "TheManagerLife/1.0 (admin@themanagerlife.com)");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Geocoding API returned non-200: " + responseCode);
                return Optional.empty();
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder responseBuilder = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseBuilder.append(inputLine);
            }
            in.close();
            conn.disconnect();

            String jsonResponse = responseBuilder.toString();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonResponse);

            if (!rootNode.isArray() || rootNode.size() == 0) {
                System.out.println("No geocoding results for address: " + address);
                return Optional.empty();
            }

            JsonNode firstResult = rootNode.get(0);
            double lat = firstResult.get("lat").asDouble();
            double lon = firstResult.get("lon").asDouble();

            return Optional.of(new double[] {lat, lon});
        } catch (Exception e) {
            System.err.println("Exception during geocoding: " + e.getMessage());
            return Optional.empty();
        }
    }
}
