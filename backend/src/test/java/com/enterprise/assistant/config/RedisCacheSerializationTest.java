package com.enterprise.assistant.config;

import com.enterprise.assistant.domain.chat.ChatSessionStatus;
import com.enterprise.assistant.domain.chat.MessageRole;
import com.enterprise.assistant.dto.response.ChatMessageDTO;
import com.enterprise.assistant.dto.response.ChatSessionDetailResponse;
import com.enterprise.assistant.dto.response.CitationDTO;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduces and guards the production failure where the session-metadata cache could not be
 * written because {@code java.time.OffsetDateTime} was unsupported by the Redis JSON serializer.
 */
class RedisCacheSerializationTest {

    private ChatSessionDetailResponse sessionDetail() {
        UUID sessionId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();

        CitationDTO citation = new CitationDTO("S1", documentId, chunkId, "Leave Policy", "leave.pdf", 1, 0.88d);
        ChatMessageDTO userMessage = new ChatMessageDTO(
                UUID.randomUUID(), MessageRole.USER, "What does our leave policy say?",
                List.of(), OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(2).truncatedTo(ChronoUnit.MILLIS));
        ChatMessageDTO assistantMessage = new ChatMessageDTO(
                UUID.randomUUID(), MessageRole.ASSISTANT, "You get 20 days [S1].",
                List.of(citation), OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1).truncatedTo(ChronoUnit.MILLIS));

        return new ChatSessionDetailResponse(
                sessionId,
                "Leave policy chat",
                ChatSessionStatus.ACTIVE,
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5).truncatedTo(ChronoUnit.MILLIS),
                OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS),
                List.of(userMessage, assistantMessage));
    }

    @Test
    void serializer_RoundTripsOffsetDateTimeAndNestedCitations() {
        GenericJackson2JsonRedisSerializer serializer = RedisConfig.redisJsonSerializer();
        ChatSessionDetailResponse original = sessionDetail();

        byte[] bytes = serializer.serialize(original);
        assertNotNull(bytes);

        String json = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(json.contains("ACTIVE"), "Enum values must survive serialization");
        assertTrue(json.contains("2026-") || json.contains("20"), "Dates must be present in the payload");

        Object restored = serializer.deserialize(bytes);
        ChatSessionDetailResponse actual = assertInstanceOf(ChatSessionDetailResponse.class, restored);

        assertEquals(original.id(), actual.id());
        assertEquals(original.title(), actual.title());
        assertEquals(original.status(), actual.status());
        assertEquals(original.createdAt(), actual.createdAt());
        assertEquals(original.updatedAt(), actual.updatedAt());
        assertEquals(2, actual.messages().size());
        assertEquals(MessageRole.ASSISTANT, actual.messages().get(1).role());
        assertEquals(original.messages().get(1).createdAt(), actual.messages().get(1).createdAt());
        assertEquals("S1", actual.messages().get(1).citations().get(0).citationId());
        assertEquals(original, actual);
    }

    @Test
    void cacheConfiguration_ValueSerializerRoundTripsSessionMetadata() {
        // Same serializer wiring RedisConfig.cacheManager uses for the "session-metadata" cache
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(RedisConfig.redisJsonSerializer()));

        ChatSessionDetailResponse original = sessionDetail();
        ByteBuffer written = cacheConfiguration.getValueSerializationPair().write(original);
        Object read = cacheConfiguration.getValueSerializationPair().read(written);

        assertEquals(original, assertInstanceOf(ChatSessionDetailResponse.class, read));
    }

    @Test
    void serializer_StillHandlesPlainCacheValues() {
        GenericJackson2JsonRedisSerializer serializer = RedisConfig.redisJsonSerializer();

        assertEquals("value", serializer.deserialize(serializer.serialize("value")));
        assertEquals(42, serializer.deserialize(serializer.serialize(42)));
    }
}
