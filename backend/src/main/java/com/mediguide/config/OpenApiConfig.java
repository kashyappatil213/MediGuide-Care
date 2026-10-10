package com.mediguide.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger documentation configuration for MediGuide Care.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mediGuideOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MediGuide Care API")
                        .description("Doctor–Patient Consultation and Follow-up Management System API documentation")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MediGuide Care Team")
                                .email("support@mediguidecare.local")));
    }
}
