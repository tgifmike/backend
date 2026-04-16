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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class EmailService {

    @Value("${mailgun.api.key}")
    private String apiKey;

    @Value("${mailgun.domain}")
    private String domain;

    private final RestTemplate restTemplate =
            new RestTemplate();


    /**
     * Plain text email
     */
    public void sendEmail(
            String to,
            String subject,
            String text
    ) {

        String url =
                "https://api.mailgun.net/v3/"
                        + domain
                        + "/messages";

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBasicAuth("api", apiKey);

        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );

        MultiValueMap<String,String> body =
                new LinkedMultiValueMap<>();

        body.add(
                "from",
                "Manager Life <mail@" + domain + ">"
        );

        body.add("to", to);

        body.add("subject", subject);

        body.add("text", text);

        HttpEntity<MultiValueMap<String,String>> request =
                new HttpEntity<>(body, headers);

        restTemplate.postForEntity(
                url,
                request,
                String.class
        );
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

        String url =
                "https://api.mailgun.net/v3/"
                        + domain
                        + "/messages";

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBasicAuth("api", apiKey);

        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );

        MultiValueMap<String,String> body =
                new LinkedMultiValueMap<>();

        body.add(
                "from",
                "Manager Life <mail@" + domain + ">"
        );

        body.add("to", to);

        body.add("subject", subject);

        body.add("text", textBody);

        body.add("html", htmlBody);


        HttpEntity<MultiValueMap<String,String>> request =
                new HttpEntity<>(body, headers);

        restTemplate.postForEntity(
                url,
                request,
                String.class
        );
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