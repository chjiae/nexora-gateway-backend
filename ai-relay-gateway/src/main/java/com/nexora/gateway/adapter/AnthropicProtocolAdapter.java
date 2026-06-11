package com.nexora.gateway.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.common.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts between Anthropic Messages API format and internal protocol.
 */
@Component
@RequiredArgsConstructor
public class AnthropicProtocolAdapter {

    private final ObjectMapper objectMapper;

    public InternalChatRequest toInternal(JsonNode anthropicRequest, String requestId) {
        InternalChatRequest.InternalChatRequestBuilder builder = InternalChatRequest.builder()
            .requestId(requestId)
            .clientProtocol("ANTHROPIC");

        if (anthropicRequest.has("model")) {
            builder.model(anthropicRequest.get("model").asText());
        }
        if (anthropicRequest.has("max_tokens")) {
            builder.maxTokens(anthropicRequest.get("max_tokens").asInt());
            builder.maxOutputTokens(anthropicRequest.get("max_tokens").asInt());
        }
        if (anthropicRequest.has("temperature")) {
            builder.temperature(anthropicRequest.get("temperature").asDouble());
        }
        if (anthropicRequest.has("top_p")) {
            builder.topP(anthropicRequest.get("top_p").asDouble());
        }
        if (anthropicRequest.has("stream")) {
            builder.stream(anthropicRequest.get("stream").asBoolean());
        }
        if (anthropicRequest.has("stop_sequences")) {
            List<String> stops = new ArrayList<>();
            anthropicRequest.get("stop_sequences").forEach(n -> stops.add(n.asText()));
            builder.stopSequences(stops);
        }
        if (anthropicRequest.has("thinking")) {
            JsonNode thinking = anthropicRequest.get("thinking");
            if (thinking.has("budget_tokens")) {
                builder.thinkingBudgetTokens(thinking.get("budget_tokens").asInt());
            }
        }

        // Parse system
        if (anthropicRequest.has("system")) {
            JsonNode system = anthropicRequest.get("system");
            if (system.isTextual()) {
                builder.system(system.asText());
            } else if (system.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode block : system) {
                    if (block.has("text")) {
                        sb.append(block.get("text").asText()).append("\n");
                    }
                }
                builder.system(sb.toString().trim());
            }
        }

        // Parse messages
        if (anthropicRequest.has("messages")) {
            List<InternalChatMessage> messages = new ArrayList<>();
            for (JsonNode msg : anthropicRequest.get("messages")) {
                String role = msg.get("role").asText();

                InternalChatMessage.InternalChatMessageBuilder msgBuilder = InternalChatMessage.builder()
                    .role(role);

                // Parse content blocks
                if (msg.has("content") && msg.get("content").isArray()) {
                    StringBuilder textContent = new StringBuilder();
                    List<InternalToolCall> toolCalls = new ArrayList<>();
                    List<Map<String, Object>> toolResults = new ArrayList<>();

                    for (JsonNode block : msg.get("content")) {
                        String blockType = block.get("type").asText();

                        switch (blockType) {
                            case "text":
                                textContent.append(block.get("text").asText());
                                break;
                            case "tool_use":
                                toolCalls.add(InternalToolCall.builder()
                                    .id(block.get("id").asText())
                                    .name(block.get("name").asText())
                                    .functionName(block.get("name").asText())
                                    .arguments(objectMapper.convertValue(block.get("input"), Map.class))
                                    .build());
                                break;
                            case "tool_result":
                                Map<String, Object> result = new LinkedHashMap<>();
                                result.put("tool_use_id", block.get("tool_use_id").asText());
                                result.put("content", block.has("content") ?
                                    block.get("content").toString() : "");
                                toolResults.add(result);
                                break;
                        }
                    }

                    msgBuilder.content(textContent.toString());
                    if (!toolCalls.isEmpty()) {
                        msgBuilder.toolCalls(toolCalls);
                    }
                    if (!toolResults.isEmpty()) {
                        msgBuilder.toolResults(toolResults);
                    }
                } else if (msg.has("content") && msg.get("content").isTextual()) {
                    msgBuilder.content(msg.get("content").asText());
                }

                messages.add(msgBuilder.build());
            }
            builder.messages(messages);
        }

        // Parse tools
        if (anthropicRequest.has("tools")) {
            List<InternalToolDefinition> tools = new ArrayList<>();
            for (JsonNode tool : anthropicRequest.get("tools")) {
                tools.add(InternalToolDefinition.builder()
                    .name(tool.get("name").asText())
                    .description(tool.has("description") ? tool.get("description").asText() : null)
                    .parameters(objectMapper.convertValue(tool.get("input_schema"), Map.class))
                    .build());
            }
            builder.tools(tools);
        }

        return builder.build();
    }

    public Map<String, Object> toAnthropicResponse(InternalChatResponse internal) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", "msg_" + internal.getId().replace("-", ""));
        response.put("type", "message");
        response.put("role", "assistant");
        response.put("model", internal.getModel());

        List<Map<String, Object>> content = new ArrayList<>();
        if (internal.getMessage() != null) {
            // Add text content
            if (internal.getMessage().getContent() != null && !internal.getMessage().getContent().isEmpty()) {
                Map<String, Object> textBlock = new LinkedHashMap<>();
                textBlock.put("type", "text");
                textBlock.put("text", internal.getMessage().getContent());
                content.add(textBlock);
            }

            // Add tool_use blocks
            if (internal.getMessage().getToolCalls() != null) {
                for (InternalToolCall tc : internal.getMessage().getToolCalls()) {
                    Map<String, Object> toolUse = new LinkedHashMap<>();
                    toolUse.put("type", "tool_use");
                    toolUse.put("id", tc.getId());
                    toolUse.put("name", tc.getFunctionName());
                    toolUse.put("input", tc.getArguments() != null ? tc.getArguments() : Map.of());
                    content.add(toolUse);
                }
            }
        }
        response.put("content", content);

        response.put("stop_reason", mapFinishReason(internal.getFinishReason()));

        if (internal.getUsage() != null) {
            Map<String, Object> usage = new LinkedHashMap<>();
            usage.put("input_tokens", internal.getUsage().getInputTokens());
            usage.put("output_tokens", internal.getUsage().getOutputTokens());
            response.put("usage", usage);
        }

        return response;
    }

    private String mapFinishReason(String finishReason) {
        if (finishReason == null) return "end_turn";
        return switch (finishReason) {
            case "stop" -> "end_turn";
            case "length" -> "max_tokens";
            case "tool_calls" -> "tool_use";
            default -> finishReason;
        };
    }
}
