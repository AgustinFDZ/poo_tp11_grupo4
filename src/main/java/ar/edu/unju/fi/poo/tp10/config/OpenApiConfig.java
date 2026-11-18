package ar.edu.unju.fi.poo.tp10.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadatos de la documentación interactiva (Swagger UI en /swagger-ui.html). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(new Info()
                .title("TP11 – API REST de Ventas")
                .version("1.1.0")
                .description("Gestión de clientes, productos y ventas (contado / financiada), "
                        + "consulta de estado de cuenta y cobro de cuotas."));
    }
}
