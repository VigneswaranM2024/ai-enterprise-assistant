package com.enterprise.assistant.service.ai;

/**
 * Service Abstraction for LLM completion and generation providers.
 */
public interface LlmService {

    /**
     * Generates a text completion for the provided prompt.
     *
     * @param userPrompt Prompt content from user or system context
     * @return Generated assistant response string
     */
    String generateResponse(String userPrompt);

    /**
     * Generates a text completion with explicit system and user prompts.
     *
     * @param systemPrompt System instructions
     * @param userPrompt User prompt content
     * @return Generated assistant response string
     */
    String generateResponse(String systemPrompt, String userPrompt);
}
