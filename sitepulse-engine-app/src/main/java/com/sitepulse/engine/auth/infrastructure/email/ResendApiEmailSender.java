package com.sitepulse.engine.auth.infrastructure.email;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sitepulse.engine.config.SitePulseProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

@Slf4j
public class ResendApiEmailSender implements ResendEmailSender {

    private final RestClient restClient;

    public ResendApiEmailSender(SitePulseProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.auth().mail().resend().baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.auth().mail().resend().apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void send(ResendOutboundEmail email) {
        ResendSendEmailResponse response = restClient.post()
                .uri("/emails")
                .body(new ResendSendEmailRequest(
                        email.from(),
                        email.to(),
                        email.subject(),
                        email.html(),
                        email.text(),
                        email.replyTo()
                ))
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        (request, responseBody) -> {
                            String body = extractBody(responseBody);
                            throw new IllegalStateException(
                                    "Resend email delivery failed with status "
                                            + responseBody.getStatusCode().value()
                                            + ": "
                                            + body
                            );
                        }
                )
                .body(ResendSendEmailResponse.class);
        log.info("Sent auth email via Resend to {} with id {}", email.to(), response == null ? null : response.id());
    }

    private String extractBody(ClientHttpResponse response) {
        try {
            return StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "<unavailable>";
        }
    }

    private record ResendSendEmailRequest(
            String from,
            List<String> to,
            String subject,
            String html,
            String text,
            @JsonProperty("reply_to") String replyTo
    ) {
    }

    private record ResendSendEmailResponse(String id) {
    }
}
