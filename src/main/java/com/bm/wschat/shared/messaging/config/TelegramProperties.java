package com.bm.wschat.shared.messaging.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {
    /**
     * Токен бота, полученный от @BotFather
     */
    private String token;
    
    /**
     * Имя бота (опционально, для проверки)
     */
    private String botUsername;
    
    /**
     * Включена ли отправка уведомлений в Telegram
     */
    private boolean enabled = false;
}
