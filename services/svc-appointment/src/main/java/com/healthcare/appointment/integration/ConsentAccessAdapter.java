package com.healthcare.appointment.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthcare.appointment.exception.ConsentAccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ConsentAccessAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentAccessAdapter.class);

    private final RestClient restClient;
    private final String path;
    private final String consentType;
    private final boolean enforcementEnabled;

    public ConsentAccessAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.consent.base-url:}") String baseUrl,
            @Value("${platform.integration.consent.path:/consents/check-access}") String path,
            @Value("${platform.integration.consent.consentType:GENERAL_CARE}") String consentType,
            @Value("${platform.integration.consent.enforcementEnabled:false}") boolean enforcementEnabled) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
        this.consentType = consentType;
        this.enforcementEnabled = enforcementEnabled;
    }

    public void ensureAccessAllowed(String patientId, String correlationId) {
        if (!enforcementEnabled) {
            return;
        }

        if (restClient == null) {
            throw new ConsentAccessDeniedException("Consent enforcement is enabled but consent service base URL is not configured");
        }

        JsonNode body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .queryParam("patientId", patientId)
                        .queryParam("consentType", consentType)
                        .build())
                .header("X-Correlation-Id", correlationId)
                .retrieve()
                .body(JsonNode.class);

        if (body == null) {
            throw new ConsentAccessDeniedException("Consent service returned an empty response");
        }

        JsonNode data = body.path("data");
        boolean allowed = data.path("accessAllowed").asBoolean(false);
        String reason = data.path("reason").asText("CONSENT_REQUIRED");

        if (!allowed) {
            throw new ConsentAccessDeniedException("Access denied by consent policy: " + reason);
        }

        LOGGER.debug("Consent access granted for patientId={} consentType={}", patientId, consentType);
    }
}
