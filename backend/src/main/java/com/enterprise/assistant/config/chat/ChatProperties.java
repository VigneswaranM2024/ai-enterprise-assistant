package com.enterprise.assistant.config.chat;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for conversation memory and chat session management.
 * Binds properties prefixed with 'chat'.
 */
@Configuration
@ConfigurationProperties(prefix = "chat")
@Getter
@Setter
public class ChatProperties {

    private Memory memory = new Memory();

    @Getter
    @Setter
    public static class Memory {
        private int maxMessages = 10;
        private int maxContextTokens = 4000;
    }
}
