package com.bm.wschat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class WsChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(WsChatApplication.class, args);
    }

}
