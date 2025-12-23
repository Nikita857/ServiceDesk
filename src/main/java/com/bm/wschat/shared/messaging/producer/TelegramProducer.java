package com.bm.wschat.shared.messaging.producer;

import com.bm.wschat.shared.messaging.config.TelegramProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;

import java.util.Map;

@Slf4j
@Service
public class TelegramProducer {

    private final TelegramProperties properties;
    private final RestClient restClient;
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot{token}/{method}";

    public TelegramProducer(TelegramProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Отправляет текстовое сообщение пользователю
     *
     * @param chatId ID чата (telegramId пользователя)
     * @param text   Текст сообщения
     */
    public void sendMessage(Long chatId, String text) {
        if (!properties.isEnabled()) {
            log.debug("Telegram notifications are disabled. Skipping message to {}", chatId);
            return;
        }

        if (chatId == null) {
            log.warn("Cannot send Telegram message: chatId is null");
            return;
        }

        try {
            String url = TELEGRAM_API_URL.replace("{token}", properties.getToken())
                    .replace("{method}", "sendMessage");

            Map<String, Object> body = Map.of(
                    "chat_id", chatId,
                    "text", text,
                    "parse_mode", "Markdown"
            );

            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Sent Telegram message to {}", chatId);

        } catch (Exception e) {
            log.error("Failed to send Telegram message to {}: {}", chatId, e.getMessage());
        }
    }
}
