package com.healthcare.medicalrecord.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("MedicalRecord Service API").version("v1").description("FHIR aligned medical record synchronization"))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .schemaRequirement("bearer-jwt", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));
    }
}
