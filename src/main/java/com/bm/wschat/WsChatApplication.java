package com.bm.wschat;

import io.github.cdimascio.dotenv.Dotenv;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@OpenAPIDefinition(info = @Info(title = "ServiceDesk API", version = "1.0"), security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT токен без префикса 'Bearer'. Пример: eyJhbGciOiJI...")
public class WsChatApplication {

        public static void main(String[] args) {
                // Загружаем переменные из .env файла в системные свойства
                Dotenv dotenv = Dotenv.configure()
                                .ignoreIfMissing()
                                .load();

                dotenv.entries().forEach(entry -> System.setProperty(
                                entry.getKey(),
                                entry.getValue()));

                SpringApplication.run(WsChatApplication.class, args);
        }

}
