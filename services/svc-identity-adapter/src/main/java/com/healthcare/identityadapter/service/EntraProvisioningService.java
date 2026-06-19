package com.healthcare.identityadapter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.identityadapter.event.PatientOnboardingRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EntraProvisioningService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntraProvisioningService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GraphAccessTokenProvider tokenProvider;
    private final String graphBaseUrl;
    private final String apiAppId;
    private final String groupName;
    private final String roleValue;
    private final String inviteRedirectUrl;

    public EntraProvisioningService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            GraphAccessTokenProvider tokenProvider,
            @Value("${platform.integration.entra.graph-base-url:https://graph.microsoft.com/v1.0}") String graphBaseUrl,
            @Value("${platform.integration.entra.api-app-id:}") String apiAppId,
            @Value("${platform.integration.entra.group-name:HCPE-PATIENT}") String groupName,
            @Value("${platform.integration.entra.role-value:PATIENT}") String roleValue,
            @Value("${platform.integration.entra.invite-redirect-url:https://myapplications.microsoft.com}") String inviteRedirectUrl) {
            this.graphBaseUrl = trimTrailingSlash(graphBaseUrl);
            this.restClient = restClientBuilder.baseUrl(this.graphBaseUrl).build();
        this.objectMapper = objectMapper;
        this.tokenProvider = tokenProvider;
        this.apiAppId = apiAppId;
        this.groupName = groupName;
        this.roleValue = roleValue;
        this.inviteRedirectUrl = inviteRedirectUrl;
    }

    public void provisionFromApproval(PatientOnboardingRequestedEvent event, String correlationId) {
        String bearerToken = tokenProvider.getAccessToken();
        GraphUser user = resolveOrInviteUser(event, correlationId, bearerToken);
        GraphGroup group = resolveGroup(correlationId, bearerToken);
        GraphServicePrincipal servicePrincipal = resolveServicePrincipal(correlationId, bearerToken);
        GraphAppRole appRole = resolveAppRole(correlationId, bearerToken);

        ensureGroupMembership(user.id(), group.id(), correlationId, bearerToken);
        ensureAppRoleAssignment(group.id(), servicePrincipal.id(), appRole.id(), correlationId, bearerToken);

        LOGGER.info("Provisioned Entra access for approved patient aggregateId={} email={} group={} role={} correlationId={}",
                event.aggregateId(), event.email(), groupName, roleValue, correlationId);
    }

    private GraphUser resolveOrInviteUser(PatientOnboardingRequestedEvent event, String correlationId, String bearerToken) {
        GraphUser user = resolveUserByEmail(event.email(), bearerToken);
        if (user != null) {
            return user;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("invitedUserEmailAddress", event.email());
        payload.put("inviteRedirectUrl", inviteRedirectUrl);
        payload.put("sendInvitationMessage", true);
        String displayName = buildDisplayName(event);
        if (!displayName.isBlank()) {
            payload.put("invitedUserDisplayName", displayName);
        }

        JsonNode invitation = postJson("/invitations", payload, bearerToken);
        JsonNode invitedUser = invitation.path("invitedUser");
        if (!invitedUser.isMissingNode() && !invitedUser.path("id").asText("").isBlank()) {
            return new GraphUser(
                    invitedUser.path("id").asText(),
                    invitedUser.path("displayName").asText(""),
                    invitedUser.path("mail").asText("")
                            .isBlank() ? event.email() : invitedUser.path("mail").asText(""),
                    invitedUser.path("userPrincipalName").asText(""),
                    invitedUser.path("userType").asText("Guest")
            );
        }

        GraphUser retriedUser = resolveUserByEmail(event.email(), bearerToken);
        if (retriedUser != null) {
            return retriedUser;
        }

        throw new IllegalStateException("Unable to resolve invited user for email=" + event.email() + " correlationId=" + correlationId);
    }

    private GraphUser resolveUserByEmail(String email, String bearerToken) {
        String filter = "mail eq '" + escapeOData(email) + "' or userPrincipalName eq '" + escapeOData(email) + "'";
        JsonNode response = getJson("/users", bearerToken,
                uriBuilder -> uriBuilder
                        .queryParam("$filter", filter)
                        .queryParam("$select", "id,displayName,mail,userPrincipalName,userType")
                        .build());

        JsonNode values = response.path("value");
        if (!values.isArray() || values.isEmpty()) {
            return null;
        }

        JsonNode first = values.get(0);
        return new GraphUser(
                first.path("id").asText(""),
                first.path("displayName").asText(""),
                first.path("mail").asText(""),
                first.path("userPrincipalName").asText(""),
                first.path("userType").asText("")
        );
    }

    private GraphGroup resolveGroup(String correlationId, String bearerToken) {
        String filter = "displayName eq '" + escapeOData(groupName) + "'";
        JsonNode response = getJson("/groups", bearerToken,
                uriBuilder -> uriBuilder
                        .queryParam("$filter", filter)
                        .queryParam("$select", "id,displayName")
                        .build());

        JsonNode values = response.path("value");
        if (values.isArray() && !values.isEmpty()) {
            JsonNode first = values.get(0);
            return new GraphGroup(first.path("id").asText(""), first.path("displayName").asText(""));
        }

        throw new IllegalStateException("Group not found groupName=" + groupName + " correlationId=" + correlationId);
    }

    private GraphServicePrincipal resolveServicePrincipal(String correlationId, String bearerToken) {
        if (apiAppId == null || apiAppId.isBlank()) {
            throw new IllegalStateException("platform.integration.entra.api-app-id is required for patient provisioning");
        }

        String filter = "appId eq '" + escapeOData(apiAppId) + "'";
        JsonNode response = getJson("/servicePrincipals", bearerToken,
                uriBuilder -> uriBuilder
                        .queryParam("$filter", filter)
                        .queryParam("$select", "id,appId,displayName")
                        .build());

        JsonNode values = response.path("value");
        if (values.isArray() && !values.isEmpty()) {
            JsonNode first = values.get(0);
            return new GraphServicePrincipal(first.path("id").asText(""), first.path("appId").asText(""), first.path("displayName").asText(""));
        }

        throw new IllegalStateException("Service principal not found apiAppId=" + apiAppId + " correlationId=" + correlationId);
    }

    private GraphAppRole resolveAppRole(String correlationId, String bearerToken) {
        String filter = "appId eq '" + escapeOData(apiAppId) + "'";
        JsonNode response = getJson("/applications", bearerToken,
                uriBuilder -> uriBuilder
                        .queryParam("$filter", filter)
                        .queryParam("$select", "appRoles")
                        .build());

        JsonNode values = response.path("value");
        if (values.isArray() && !values.isEmpty()) {
            JsonNode first = values.get(0);
            JsonNode appRoles = first.path("appRoles");
            if (appRoles.isArray()) {
                for (JsonNode appRole : appRoles) {
                    if (roleValue.equalsIgnoreCase(appRole.path("value").asText(""))) {
                        return new GraphAppRole(appRole.path("id").asText(""), appRole.path("value").asText(""));
                    }
                }
            }
        }

        throw new IllegalStateException("App role not found roleValue=" + roleValue + " apiAppId=" + apiAppId + " correlationId=" + correlationId);
    }

    private void ensureGroupMembership(String userId, String groupId, String correlationId, String bearerToken) {
        JsonNode memberOf = getJson("/users/" + userId + "/memberOf", bearerToken,
                uriBuilder -> uriBuilder.queryParam("$select", "id").build());
        JsonNode values = memberOf.path("value");
        if (values.isArray()) {
            for (JsonNode entry : values) {
                if (groupId.equalsIgnoreCase(entry.path("id").asText(""))) {
                    return;
                }
            }
        }

        Map<String, Object> payload = Map.of("@odata.id", graphBaseUrl + "/directoryObjects/" + userId);
        postVoid("/groups/" + groupId + "/members/$ref", payload, bearerToken);
        LOGGER.info("Added user to Entra group userId={} groupId={} correlationId={}", userId, groupId, correlationId);
    }

    private void ensureAppRoleAssignment(String groupId, String resourceId, String appRoleId, String correlationId, String bearerToken) {
        JsonNode assignments = getJson("/servicePrincipals/" + resourceId + "/appRoleAssignedTo", bearerToken,
                uriBuilder -> uriBuilder.queryParam("$select", "principalId,resourceId,appRoleId").build());
        JsonNode values = assignments.path("value");
        if (values.isArray()) {
            for (JsonNode entry : values) {
                if (groupId.equalsIgnoreCase(entry.path("principalId").asText(""))
                        && resourceId.equalsIgnoreCase(entry.path("resourceId").asText(""))
                        && appRoleId.equalsIgnoreCase(entry.path("appRoleId").asText(""))) {
                    return;
                }
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("principalId", groupId);
        payload.put("resourceId", resourceId);
        payload.put("appRoleId", appRoleId);
        postVoid("/servicePrincipals/" + resourceId + "/appRoleAssignedTo", payload, bearerToken);
        LOGGER.info("Assigned Entra app role principalId={} resourceId={} appRoleId={} correlationId={}",
                groupId, resourceId, appRoleId, correlationId);
    }

    private JsonNode getJson(String path, String bearerToken, java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI> uriFunction) {
        String body = restClient.get()
                .uri(uriBuilder -> uriFunction.apply(uriBuilder.path(path)))
                .header("Authorization", bearerTokenHeader(bearerToken))
                .retrieve()
                .body(String.class);
        return parseJson(body);
    }

    private JsonNode postJson(String path, Map<String, Object> payload, String bearerToken) {
        String body = restClient.post()
                .uri(path)
                .header("Authorization", bearerTokenHeader(bearerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
        return parseJson(body);
    }

    private void postVoid(String path, Map<String, Object> payload, String bearerToken) {
        restClient.post()
                .uri(path)
                .header("Authorization", bearerTokenHeader(bearerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    private JsonNode parseJson(String body) {
        try {
            if (body == null || body.isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse Microsoft Graph response", ex);
        }
    }

    private String bearerTokenHeader(String bearerToken) {
        return "Bearer " + bearerToken;
    }

    private String escapeOData(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private String buildDisplayName(PatientOnboardingRequestedEvent event) {
        String first = event.givenName() == null ? "" : event.givenName().trim();
        String last = event.familyName() == null ? "" : event.familyName().trim();
        return (first + " " + last).trim();
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://graph.microsoft.com/v1.0";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private record GraphUser(String id, String displayName, String mail, String userPrincipalName, String userType) {
    }

    private record GraphGroup(String id, String displayName) {
    }

    private record GraphServicePrincipal(String id, String appId, String displayName) {
    }

    private record GraphAppRole(String id, String value) {
    }
}