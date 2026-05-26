package com.healthcare.platform.common.security;

import com.azure.security.keyvault.secrets.SecretClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class KeyVaultSecretProvider {
    private final ObjectProvider<SecretClient> secretClientProvider;

    public KeyVaultSecretProvider(ObjectProvider<SecretClient> secretClientProvider) {
        this.secretClientProvider = secretClientProvider;
    }

    public String resolveSecret(String secretName, String fallback) {
        SecretClient client = secretClientProvider.getIfAvailable();
        if (client == null) {
            return fallback;
        }
        return client.getSecret(secretName).getValue();
    }
}
