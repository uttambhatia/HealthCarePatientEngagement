package com.healthcare.identityadapter.service;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.identity.CommunicationIdentityClient;
import com.azure.communication.identity.CommunicationIdentityClientBuilder;
import com.azure.communication.identity.models.CommunicationTokenScope;
import com.azure.communication.identity.models.CommunicationUserIdentifierAndToken;
import com.azure.communication.sms.SmsClient;
import com.azure.communication.sms.SmsClientBuilder;
import com.azure.communication.sms.models.SmsSendOptions;
import com.azure.communication.sms.models.SmsSendResult;
import com.azure.core.credential.AzureKeyCredential;
import com.healthcare.identityadapter.dto.AcsNotificationRequest;
import com.healthcare.identityadapter.dto.AcsTeleconsultSessionRequest;
import com.healthcare.identityadapter.dto.AcsTeleconsultSessionResponse;
import com.healthcare.identityadapter.dto.AcsTeleconsultTokenRequest;
import com.healthcare.identityadapter.dto.AcsTeleconsultTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class AcsIntegrationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcsIntegrationService.class);

    private final EmailClient emailClient;
    private final String emailFromAddress;
    private final SmsClient smsClient;
    private final String smsFromNumber;
    private final CommunicationIdentityClient communicationIdentityClient;
    private final String teleconsultJoinBaseUrl;
    private final String doctorRoleQuery;
    private final String patientRoleQuery;

    public AcsIntegrationService(
            @Value("${platform.integration.acs.email.endpoint:}") String emailEndpoint,
            @Value("${platform.integration.acs.email.access-key:}") String emailAccessKey,
            @Value("${platform.integration.acs.email.from-address:}") String emailFromAddress,
            @Value("${platform.integration.acs.sms.endpoint:}") String smsEndpoint,
            @Value("${platform.integration.acs.sms.access-key:}") String smsAccessKey,
            @Value("${platform.integration.acs.sms.from-number:}") String smsFromNumber,
            @Value("${platform.integration.acs.identity.connection-string:}") String acsIdentityConnectionString,
            @Value("${platform.integration.acs.teleconsult.join-base-url:https://teleconsult.healthcare.local/session}") String teleconsultJoinBaseUrl,
            @Value("${platform.integration.acs.teleconsult.doctor-role-query:role=DOCTOR}") String doctorRoleQuery,
            @Value("${platform.integration.acs.teleconsult.patient-role-query:role=PATIENT}") String patientRoleQuery) {
        this.emailClient = buildEmailClient(emailEndpoint, emailAccessKey);
        this.emailFromAddress = emailFromAddress == null ? "" : emailFromAddress.trim();
        this.smsClient = buildSmsClient(smsEndpoint, smsAccessKey);
        this.smsFromNumber = smsFromNumber == null ? "" : smsFromNumber.trim();
        this.communicationIdentityClient = buildCommunicationIdentityClient(acsIdentityConnectionString);
        this.teleconsultJoinBaseUrl = trimTrailingSlash(teleconsultJoinBaseUrl);
        this.doctorRoleQuery = doctorRoleQuery;
        this.patientRoleQuery = patientRoleQuery;
    }

    public void acceptNotification(AcsNotificationRequest request, String correlationId) {
        if (smsClient != null && !smsFromNumber.isBlank() && isSmsChannel(request.channel()) && looksLikePhone(request.recipientId())) {
            SmsSendResult result = smsClient.send(
                    smsFromNumber,
                    request.recipientId(),
                    request.message(),
                    new SmsSendOptions());

            if (!result.isSuccessful()) {
                throw new IllegalStateException("ACS SMS dispatch failed id=" + request.id() + " error=" + result.getHttpStatusCode());
            }

            LOGGER.info("Dispatched ACS sms id={} recipient={} correlationId={}",
                    request.id(), request.recipientId(), correlationId);
            return;
        }

        if (emailClient != null && !emailFromAddress.isBlank() && isEmailChannel(request.channel()) && looksLikeEmail(request.recipientId())) {
                String subject = buildSubject(request);
                EmailMessage message = new EmailMessage()
                    .setSenderAddress(emailFromAddress)
                    .setSubject(subject)
                    .setBodyPlainText(request.message())
                    .setToRecipients(List.of(new EmailAddress(request.recipientId())));

            emailClient.beginSend(message).waitForCompletion();
            LOGGER.info("Dispatched ACS email id={} recipient={} correlationId={}",
                    request.id(), request.recipientId(), correlationId);
            return;
        }

        LOGGER.info("Accepted ACS notification dispatch request id={} recipientId={} channel={} correlationId={}",
                request.id(), request.recipientId(), request.channel(), correlationId);

        if (emailClient == null || emailFromAddress.isBlank()) {
            LOGGER.warn("ACS delivery skipped id={} correlationId={} because ACS config for channel={} is incomplete",
                    request.id(), correlationId, request.channel());
        } else if (isSmsChannel(request.channel()) && (smsClient == null || smsFromNumber.isBlank())) {
            LOGGER.warn("ACS sms delivery skipped id={} correlationId={} because ACS sms config is incomplete",
                    request.id(), correlationId);
        } else if (isSmsChannel(request.channel()) && !looksLikePhone(request.recipientId())) {
            LOGGER.warn("ACS sms delivery skipped id={} correlationId={} because recipientId is not a phone: {}",
                    request.id(), correlationId);
        } else if (!isEmailChannel(request.channel())) {
            LOGGER.warn("ACS delivery skipped id={} correlationId={} because channel={} is unsupported",
                    request.id(), correlationId, request.channel());
        } else if (!looksLikeEmail(request.recipientId())) {
            LOGGER.warn("ACS email delivery skipped id={} correlationId={} because recipientId is not an email: {}",
                    request.id(), correlationId, request.recipientId());
        }
    }

    public AcsTeleconsultSessionResponse createTeleconsultSession(AcsTeleconsultSessionRequest request, String correlationId) {
        String seed = String.join("|", request.appointmentId(), request.patientId(), request.providerId());
        String sessionId = UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();

        String doctorJoinUrl = joinUrl(sessionId, doctorRoleQuery);
        String patientJoinUrl = joinUrl(sessionId, patientRoleQuery);

        LOGGER.info("Provisioned ACS teleconsult session sessionId={} appointmentId={} correlationId={}",
                sessionId, request.appointmentId(), correlationId);

        return new AcsTeleconsultSessionResponse(sessionId, doctorJoinUrl, patientJoinUrl);
    }

    public AcsTeleconsultTokenResponse issueTeleconsultToken(AcsTeleconsultTokenRequest request, String correlationId) {
        String normalizedRole = request.role().trim().toUpperCase();
        String roleQuery = "PATIENT".equals(normalizedRole) ? patientRoleQuery : doctorRoleQuery;
        String joinUrl = joinUrl(request.sessionId().trim(), roleQuery);

        if (communicationIdentityClient == null) {
            String accessToken = "acs-bootstrap-" + UUID.randomUUID();
            String expiresAt = OffsetDateTime.now().plusHours(1).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            LOGGER.warn("ACS identity connection string is not configured; issued placeholder teleconsult token sessionId={} role={} correlationId={}",
                request.sessionId(), normalizedRole, correlationId);
            return new AcsTeleconsultTokenResponse(
                request.sessionId().trim(),
                normalizedRole,
                accessToken,
                "Bearer",
                expiresAt,
                joinUrl,
                "IDENTITY_ADAPTER_PLACEHOLDER"
            );
        }

        CommunicationUserIdentifierAndToken userAndToken = communicationIdentityClient
            .createUserAndToken(Collections.singletonList(CommunicationTokenScope.VOIP));

        String accessToken = userAndToken.getUserToken().getToken();
        String expiresAt = userAndToken.getUserToken().getExpiresAt() == null
            ? OffsetDateTime.now().plusHours(1).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            : userAndToken.getUserToken().getExpiresAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        LOGGER.info("Issued ACS teleconsult token sessionId={} role={} correlationId={}",
            request.sessionId(), normalizedRole, correlationId);

        return new AcsTeleconsultTokenResponse(
            request.sessionId().trim(),
            normalizedRole,
            accessToken,
            "Bearer",
            expiresAt,
            joinUrl,
            "ACS_IDENTITY"
        );
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://teleconsult.healthcare.local/session";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String joinUrl(String sessionId, String query) {
        String sanitizedQuery = query == null ? "" : query.trim();
        if (sanitizedQuery.isEmpty()) {
            return teleconsultJoinBaseUrl + "/" + sessionId;
        }
        return teleconsultJoinBaseUrl + "/" + sessionId + "?" + sanitizedQuery;
    }

    private EmailClient buildEmailClient(String endpoint, String accessKey) {
        if (endpoint == null || endpoint.isBlank() || accessKey == null || accessKey.isBlank()) {
            return null;
        }
        return new EmailClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(accessKey))
                .buildClient();
    }

    private SmsClient buildSmsClient(String endpoint, String accessKey) {
        if (endpoint == null || endpoint.isBlank() || accessKey == null || accessKey.isBlank()) {
            return null;
        }
        return new SmsClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(accessKey))
                .buildClient();
    }

    private CommunicationIdentityClient buildCommunicationIdentityClient(String connectionString) {
        if (connectionString == null || connectionString.isBlank()) {
            return null;
        }

        return new CommunicationIdentityClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    private boolean isEmailChannel(String channel) {
        return channel != null && "EMAIL".equalsIgnoreCase(channel.trim());
    }

    private boolean isSmsChannel(String channel) {
        return channel != null && "SMS".equalsIgnoreCase(channel.trim());
    }

    private boolean looksLikeEmail(String value) {
        return value != null && value.contains("@");
    }

    private boolean looksLikePhone(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.startsWith("+") && trimmed.length() >= 8;
    }

    private String buildSubject(AcsNotificationRequest request) {
        if (request.templateId() == null || request.templateId().isBlank()) {
            return "Healthcare Notification";
        }
        return "Healthcare Notification - " + request.templateId();
    }
}