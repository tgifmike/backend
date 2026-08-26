//package com.backend.backend.service;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.client.RestTemplate;
//
//@Service
//public class EmailService {
//
//    @Value("${mailgun.api.key}")
//    private String apiKey;
//
//    @Value("${mailgun.domain}")
//    private String domain;
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    public void sendEmail(String to, String subject, String text) {
//
//        String url = "https://api.mailgun.net/v3/" + domain + "/messages";
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBasicAuth("api", apiKey);
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
//        body.add("from", "Manager Life <mail@" + domain + ">");
//        body.add("to", to);
//        body.add("subject", subject);
//        body.add("text", text);
//
//        HttpEntity<MultiValueMap<String, String>> request =
//                new HttpEntity<>(body, headers);
//
//        restTemplate.postForEntity(url, request, String.class);
//    }
//    // Contact form
//    public void sendContactEmail(String name, String email, String message) {
//        String subject = "Contact Form Submission";
//        String body = String.format(
//                "Name: %s%nEmail: %s%n%nMessage:%n%s",
//                name, email, message
//        );
//        sendEmail("admin@themanagerlife.com", subject, body);
//    }
//
//    // Sales inquiry
//    public void sendSalesEmail(String name, String email, String restaurant, Integer locations, String message) {
//        String subject = "Sales Inquiry: " + restaurant;
//        String body = String.format(
//                "Name: %s%nEmail: %s%nRestaurant: %s%nLocations: %d%n%nMessage:%n%s",
//                name, email, restaurant, locations, message
//        );
//        sendEmail("admin@themanagerlife.com", subject, body);
//    }
//
//    public void sendFreeTrialEmail(String name, String email, String restaurant, Integer locations, String message) {
//        String subject = "Free Trial Request: " + restaurant;
//        String body = String.format(
//                "Name: %s%nEmail: %s%nRestaurant: %s%nLocations: %d%n%nMessage:%n%s",
//                name, email, restaurant, locations, message != null ? message : "N/A"
//        );
//        sendEmail("admin@themanagerlife.com", subject, body);
//    }
//
//
//}
package com.backend.backend.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
public class EmailService {

    @Value("${MAILGUNAPI}")
    private String apiKey;

    @Value("${MAILGUNDOMAIN}")
    private String domain;

    @Value("${mailgun.base-url:https://api.mailgun.net}")
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @PostConstruct
    void logMailgunConfiguration() {
        String key = normalizedApiKey();
        log.info(
                "Mailgun configured: domain={}, baseUrl={}, keyLength={}, keyFingerprint={}",
                normalizedDomain(),
                baseUrl,
                key.length(),
                fingerprint(key)
        );
    }


    /**
     * Plain text email
     */
    public void sendEmail(
            String to,
            String subject,
            String text
    ) {

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("from", "Manager Life <mail@" + normalizedDomain() + ">");
        fields.put("to", to);
        fields.put("subject", subject);
        fields.put("text", text);

        sendMailgunRequest(fields);
    }


    /**
     * Multipart email (HTML + plaintext fallback)
     */
    public void sendMultipartEmail(
            String to,
            String subject,
            String textBody,
            String htmlBody
    ) {

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("from", "Manager Life <mail@" + normalizedDomain() + ">");
        fields.put("to", to);
        fields.put("subject", subject);
        fields.put("text", textBody);
        fields.put("html", htmlBody);

        sendMailgunRequest(fields);
    }

    private String buildMessagesUrl() {
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        if (normalizedBaseUrl.isEmpty()) {
            throw new IllegalStateException("Mailgun base URL is not configured");
        }

        return normalizedBaseUrl.replaceAll("/+$", "")
                + "/v3/"
                + normalizedDomain()
                + "/messages";
    }

    private String mailgunAuthorization() {
        String credentials = "api:" + normalizedApiKey();
        String encodedCredentials = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)
        );
        return "Basic " + encodedCredentials;
    }

    private void sendMailgunRequest(Map<String, String> fields) {
        String boundary = "----ManagerLife" + UUID.randomUUID();
        String body = buildMultipartBody(fields, boundary);

        HttpRequest request = HttpRequest.newBuilder(URI.create(buildMessagesUrl()))
                .header(HttpHeaders.AUTHORIZATION, mailgunAuthorization())
                .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=" + boundary)
                .header(HttpHeaders.ACCEPT, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String responseBody = response.body() == null ? "" : response.body();
                throw new RestClientResponseException(
                        "Mailgun request failed",
                        response.statusCode(),
                        HttpStatusCode.valueOf(response.statusCode()).toString(),
                        HttpHeaders.EMPTY,
                        responseBody.getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8
                );
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RestClientException("Mailgun request was interrupted", ex);
        } catch (IOException ex) {
            throw new RestClientException("Mailgun request failed", ex);
        }
    }

    private String buildMultipartBody(Map<String, String> fields, String boundary) {
        StringBuilder body = new StringBuilder();

        fields.forEach((name, value) -> body
                .append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"")
                .append(name)
                .append("\"\r\n\r\n")
                .append(value == null ? "" : value)
                .append("\r\n"));

        return body.append("--")
                .append(boundary)
                .append("--\r\n")
                .toString();
    }

    private String normalizedApiKey() {
        String normalized = apiKey == null ? "" : apiKey.trim();
        if (normalized.isEmpty()) {
            throw new IllegalStateException("Mailgun API key is not configured");
        }
        return normalized;
    }

    private String normalizedDomain() {
        String normalized = domain == null ? "" : domain.trim();
        if (normalized.isEmpty()) {
            throw new IllegalStateException("Mailgun domain is not configured");
        }
        return normalized;
    }

    private String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }


    /**
     * Contact form email
     */
    public void sendContactEmail(
            String name,
            String email,
            String message
    ) {

        String subject =
                "Contact Form Submission";

        String body =
                String.format(
                        "Name: %s%nEmail: %s%n%nMessage:%n%s",
                        name,
                        email,
                        message
                );

        sendEmail(
                "admin@themanagerlife.com",
                subject,
                body
        );
    }


    /**
     * Sales inquiry email
     */
    public void sendSalesEmail(
            String name,
            String email,
            String restaurant,
            Integer locations,
            String message
    ) {

        String subject =
                "Sales Inquiry: "
                        + restaurant;

        String body =
                String.format(
                        "Name: %s%nEmail: %s%nRestaurant: %s%nLocations: %d%n%nMessage:%n%s",
                        name,
                        email,
                        restaurant,
                        locations,
                        message
                );

        sendEmail(
                "admin@themanagerlife.com",
                subject,
                body
        );
    }


    /**
     * Free trial request email
     */
    public void sendFreeTrialEmail(
            String name,
            String email,
            String restaurant,
            Integer locations,
            String message
    ) {

        String subject =
                "Free Trial Request: "
                        + restaurant;

        String body =
                String.format(
                        "Name: %s%nEmail: %s%nRestaurant: %s%nLocations: %d%n%nMessage:%n%s",
                        name,
                        email,
                        restaurant,
                        locations,
                        message != null
                                ? message
                                : "N/A"
                );

        sendEmail(
                "admin@themanagerlife.com",
                subject,
                body
        );
    }
}
