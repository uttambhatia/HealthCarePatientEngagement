package com.healthcare.platform.common.azure;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureClientConfiguration {
    @Bean
    public DefaultAzureCredential defaultAzureCredential() {
        return new DefaultAzureCredentialBuilder().build();
    }

    @Bean
    public ServiceBusClientBuilder serviceBusClientBuilder(DefaultAzureCredential credential,
                                                          @Value("${platform.azure.servicebus.fqdn:}") String fqdn) {
        ServiceBusClientBuilder builder = new ServiceBusClientBuilder();
        if (!fqdn.isBlank()) {
            builder.credential(fqdn, credential);
        }
        return builder;
    }

    @Bean
    public EventHubClientBuilder eventHubClientBuilder(DefaultAzureCredential credential,
                                                       @Value("${platform.azure.eventhub.fqdn:}") String fqdn) {
        EventHubClientBuilder builder = new EventHubClientBuilder();
        if (!fqdn.isBlank()) {
            builder.credential(fqdn, "care-coordination", credential);
        }
        return builder;
    }

    @Bean
    public SecretClient secretClient(DefaultAzureCredential credential,
                                     @Value("${platform.azure.keyvault.url:}") String vaultUrl) {
        if (vaultUrl == null || vaultUrl.isBlank()) {
            return null;
        }
        return new SecretClientBuilder().vaultUrl(vaultUrl).credential(credential).buildClient();
    }
}
