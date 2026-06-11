package com.nexora.gateway.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.common.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Converts between OpenAI API format and internal protocol.
 */
@Component
@RequiredArgsConstructor
public class OpenAIProtocolAdapter {

    private final ObjectMapper objectMapper;

    public InternalChatRequest toInternal(JsonNode openaiRequest, String requestId) {
        InternalChatRequest.InternalChatRequestBuilder builder = InternalChatRequest.builder()
            .requestId(requestId)
            .clientProtocol("OPENAI");

        if (openaiRequest.has("model")) {
            builder.model(openaiRequest.get("model").asText());
        }
        if (openaiRequest.has("temperature")) {
            builder.temperature(openaiRequest.get("temperature").asDouble());
        }
        if (openaiRequest.has("top_p")) {
            builder.topP(openaiRequest.get("top_p").asDouble());
        }
        if (openaiRequest.has("max_tokens")) {
            builder.maxTokens(openaiRequest.get("max_tokens").asInt());
        }
        if (openaiRequest.has("max_completion_tokens")) {
            builder.maxOutputTokens(openaiRequest.get("max_completion_tokens").asInt());
        }
        if (openaiRequest.has("stream")) {
            builder.stream(openaiRequest.get("stream").asBoolean());
        }
        if (openaiRequest.has("stop")) {
            JsonNode stop = openaiRequest.get("stop");
            if (stop.isArray()) {
                builder.stopSequences(StreamSupport.stream(stop.spliterator(), false)
                    .map(JsonNode::asText).collect(Collectors.toList()));
            } else {
                builder.stop(stop.asText());
            }
        }
        if (openaiRequest.has("presence_penalty")) {
            builder.presencePenalty(openaiRequest.get("presence_penalty").asDouble());
        }
        if (openaiRequest.has("frequency_penalty")) {
            builder.frequencyPenalty(openaiRequest.get("frequency_penalty").asDouble());
        }
        if (openaiRequest.has("seed")) {
            builder.seed(openaiRequest.get("seed").asInt());
        }
        if (openaiRequest.has("user")) {
            builder.user(openaiRequest.get("user").asText());
        }

        // Parse messages
        if (openaiRequest.has("messages")) {
            List<InternalChatMessage> messages = new ArrayList<>();
            String systemMessage = null;

            for (JsonNode msg : openaiRequest.get("messages")) {
                String role = msg.get("role").asText();

                if ("system".equals(role)) {
                    if (msg.has("content") && msg.get("content").isTextual()) {
                        systemMessage = msg.get("content").asText();
                    }
                    continue;
                }

                InternalChatMessage.InternalChatMessageBuilder msgBuilder = InternalChatMessage.builder()
                    .role(role);

                if (msg.has("content")) {
                    if (msg.get("content").isTextual()) {
                        msgBuilder.content(msg.get("content").asText());
                    } else if (msg.get("content").isArray()) {
                        // Handle multimodal content
                        StringBuilder textContent = new StringBuilder();
                        for (JsonNode part : msg.get("content")) {
                            if (part.has("text")) {
                                textContent.append(part.get("text").asText());
                            }
                        }
                        msgBuilder.content(textContent.toString());
                    }
                }

                // Parse tool_calls
                if (msg.has("tool_calls")) {
                    List<InternalToolCall> toolCalls = new ArrayList<>();
                    for (JsonNode tc : msg.get("tool_calls")) {
                        InternalToolCall call = InternalToolCall.builder()
                            .id(tc.get("id").asText())
                            .type(tc.get("type").asText())
                            .functionName(tc.get("function").get("name").asText())
                            .arguments(parseJson(tc.get("function").get("arguments").asText()))
                            .index(tc.has("index") ? tc.get("index").asInt() : 0)
                            .build();
                        toolCalls.add(call);
                    }
                    msgBuilder.toolCalls(toolCalls);
                }

                // Tool role message (tool result)
                if ("tool".equals(role)) {
                    msgBuilder.toolCallId(msg.get("tool_call_id").asText());
                }

                messages.add(msgBuilder.build());
            }

            builder.messages(messages);
            builder.system(systemMessage);
        }

        // Parse tools
        if (openaiRequest.has("tools")) {
            List<InternalToolDefinition> tools = new ArrayList<>();
            for (JsonNode tool : openaiRequest.get("tools")) {
                tools.add(InternalToolDefinition.builder()
                    .type(tool.get("type").asText())
                    .name(tool.get("function").get("name").asText())
                    .description(tool.get("function").has("description") ?
                        tool.get("function").get("description").asText() : null)
                    .parameters(parseJson(tool.get("function").get("parameters").toString()))
                    .build());
            }
            builder.tools(tools);
        }
        if (openaiRequest.has("tool_choice")) {
            builder.toolChoice(openaiRequest.get("tool_choice").asText());
        }

        return builder.build();
    }

    public Map<String, Object> toOpenAIResponse(InternalChatResponse internal) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", internal.getId());
        response.put("object", "chat.completion");
        response.put("created", System.currentTimeMillis() / 1000);
        response.put("model", internal.getModel());

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant");
        if (internal.getMessage() != null) {
            message.put("content", internal.getMessage().getContent());
            if (internal.getMessage().getToolCalls() != null) {
                List<Map<String, Object>> toolCalls = internal.getMessage().getToolCalls().stream()
                    .map(tc -> {
                        Map<String, Object> call = new LinkedHashMap<>();
                        call.put("id", tc.getId());
                        call.put("type", "function");
                        Map<String, Object> func = new LinkedHashMap<>();
                        func.put("name", tc.getFunctionName());
                        func.put("arguments", tc.getArguments() != null ?
                            objectMapper.convertValue(tc.getArguments(), String.class) : "{}");
                        call.put("function", func);
                        return call;
                    }).collect(Collectors.toList());
                message.put("tool_calls", toolCalls);
            }
        }

        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("message", message);
        choice.put("finish_reason", internal.getFinishReason() != null ? internal.getFinishReason() : "stop");
        choices.add(choice);

        response.put("choices", choices);

        if (internal.getUsage() != null) {
            Map<String, Object> usage = new LinkedHashMap<>();
            usage.put("prompt_tokens", internal.getUsage().getInputTokens());
            usage.put("completion_tokens", internal.getUsage().getOutputTokens());
            usage.put("total_tokens", internal.getUsage().getTotalTokens());
            response.put("usage", usage);
        }

        return response;
    }

    private Map<String, Object> parseJson(String json) {
        try {
            if (json == null || json.isEmpty()) return Map.of();
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
