package com.neodent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI neodentOpenAPI() {

        return new OpenAPI()
            .info(
                new Info()
                    .title("NeoDent REST API")
                    .description(
                        """
                        API REST del sistema NeoDent.

                        Incluye gestión de pacientes, agenda,
                        atención odontológica, seguridad,
                        pagos, documentos e integraciones externas.
                        """
                    )
                    .version("1.0.0")
                    .contact(
                        new Contact()
                            .name("Equipo NeoDent")
                    )
            );
    }
}