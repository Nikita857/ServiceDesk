package com.bm.wschat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class WsChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(WsChatApplication.class, args);
//        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
//
//        System.out.println("=== Генерация паролей для миграций ===");
//        System.out.println("admin / Admin123!@#");
//        System.out.println(encoder.encode("Admin123!@#"));
//        System.out.println();
//
//        System.out.println("specialist1 / Spec123!");
//        System.out.println(encoder.encode("Spec123!"));
//        System.out.println();
//
//        System.out.println("user1 / User123!");
//        System.out.println(encoder.encode("User123!"));
//
//        // Закрой приложение
//        System.exit(0);
    }

}
