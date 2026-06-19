package com.healthcare.identityadapter.service;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import org.springframework.stereotype.Component;

@Component
public class DefaultGraphAccessTokenProvider implements GraphAccessTokenProvider {
    private static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";

    private final DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();

    @Override
    public String getAccessToken() {
        AccessToken token = credential.getToken(new TokenRequestContext().addScopes(GRAPH_SCOPE)).block();
        if (token == null || token.getToken() == null || token.getToken().isBlank()) {
            throw new IllegalStateException("Unable to acquire Microsoft Graph access token");
        }
        return token.getToken();
    }
}