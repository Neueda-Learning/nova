package com.nova.portfolio.ai.client;

import com.nova.portfolio.config.AiProperties;
import com.nova.portfolio.exception.AiServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/** Talks to any OpenAI-compatible /chat/completions endpoint (DeepSeek by default config). */
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final RestTemplate aiRestTemplate;
    private final AiProperties properties;

    public OpenAiCompatibleLlmClient(@Qualifier("aiRestTemplate") RestTemplate aiRestTemplate, AiProperties properties) {
        this.aiRestTemplate = aiRestTemplate;
        this.properties = properties;
    }

    @Override
    public boolean isAvailable() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            throw new AiServiceException("AI service is not configured", HttpStatus.SERVICE_UNAVAILABLE.value());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        Map<String, Object> body = Map.of(
            "model", properties.getModel(),
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "temperature", 0.3,
            "stream", false
        );

        String url = properties.getBaseUrl() + "/chat/completions";
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = aiRestTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
            return extractContent(response);
        } catch (HttpClientErrorException e) {
            throw new AiServiceException("AI client error: " + e.getStatusCode(), e.getStatusCode().value());
        } catch (HttpServerErrorException e) {
            throw new AiServiceException("AI server error: " + e.getStatusCode(), e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new AiServiceException("AI network error", HttpStatus.BAD_GATEWAY.value(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        Object choicesObj = response == null ? null : response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            throw new AiServiceException("AI service returned no choices", HttpStatus.BAD_GATEWAY.value());
        }
        Object firstChoice = choices.get(0);
        Object message = firstChoice instanceof Map<?, ?> choiceMap ? choiceMap.get("message") : null;
        Object content = message instanceof Map<?, ?> messageMap ? messageMap.get("content") : null;
        if (content == null) {
            throw new AiServiceException("AI service returned no content", HttpStatus.BAD_GATEWAY.value());
        }
        return content.toString();
    }
}
