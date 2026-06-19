package com.healthcare.identityadapter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.identityadapter.event.PatientOnboardingRequestedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EntraProvisioningServiceTest {

    @Test
    void shouldInviteResolveAndAssignPatientRole() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GraphAccessTokenProvider tokenProvider = () -> "test-token";

        server.expect(once(), requestTo("http://graph.example.test/v1.0/users?$filter=mail%20eq%20'ava.approved@example.com'%20or%20userPrincipalName%20eq%20'ava.approved@example.com'&$select=id,displayName,mail,userPrincipalName,userType"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess("{\"value\":[]}", org.springframework.http.MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://graph.example.test/v1.0/invitations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(jsonPath("$.invitedUserEmailAddress").value("ava.approved@example.com"))
                .andRespond(withSuccess("{\"invitedUser\":{\"id\":\"user-1\",\"mail\":\"ava.approved@example.com\"}}", org.springframework.http.MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://graph.example.test/v1.0/groups?$filter=displayName%20eq%20'HCPE-PATIENT'&$select=id,displayName"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess("{\"value\":[{\"id\":\"group-1\",\"displayName\":\"HCPE-PATIENT\"}]}", org.springframework.http.MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://graph.example.test/v1.0/servicePrincipals?$filter=appId%20eq%20'api-app-1'&$select=id,appId,displayName"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess("{\"value\":[{\"id\":\"sp-1\",\"appId\":\"api-app-1\",\"displayName\":\"hcpe-api-backend\"}]}", org.springframework.http.MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://graph.example.test/v1.0/applications?$filter=appId%20eq%20'api-app-1'&$select=appRoles"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess("{\"value\":[{\"appRoles\":[{\"id\":\"role-1\",\"value\":\"PATIENT\"}]}]}", org.springframework.http.MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://graph.example.test/v1.0/users/user-1/memberOf?$select=id"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess("{\"value\":[]}", org.springframework.http.MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://graph.example.test/v1.0/groups/group-1/members/$ref"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess());

        server.expect(once(), requestTo("http://graph.example.test/v1.0/servicePrincipals/sp-1/appRoleAssignedTo?$select=principalId,resourceId,appRoleId"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess("{\"value\":[]}", org.springframework.http.MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://graph.example.test/v1.0/servicePrincipals/sp-1/appRoleAssignedTo"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(jsonPath("$.principalId").value("group-1"))
                .andExpect(jsonPath("$.resourceId").value("sp-1"))
                .andExpect(jsonPath("$.appRoleId").value("role-1"))
                .andRespond(withSuccess("{}", org.springframework.http.MediaType.APPLICATION_JSON));

        EntraProvisioningService service = new EntraProvisioningService(
                builder,
                new ObjectMapper(),
                tokenProvider,
                "http://graph.example.test/v1.0",
                "api-app-1",
                "HCPE-PATIENT",
                "PATIENT",
                "https://myapplications.microsoft.com"
        );

        service.provisionFromApproval(
                new PatientOnboardingRequestedEvent("patient-1", "EXT-300", "Ava", "Approved", "ava.approved@example.com", "PATIENT"),
                "corr-900");

        server.verify();
    }
}