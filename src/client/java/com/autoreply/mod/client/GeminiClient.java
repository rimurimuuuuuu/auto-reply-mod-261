package com.autoreply.mod.client;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class GeminiClient {

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    public static CompletableFuture<String> generateReply(String apiKey, String systemPrompt, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String requestBody = buildRequestBody(systemPrompt, message);
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL + "?key=" + apiKey))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .timeout(Duration.ofSeconds(15))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return parseResponse(response.body());
            } catch (Exception e) {
                return null;
            }
        });
    }

    private static String buildRequestBody(String systemPrompt, String userMessage) {
        JsonObject root = new JsonObject();

        JsonObject systemObj = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sysPart = new JsonObject();
        sysPart.addProperty("text", systemPrompt);
        sysParts.add(sysPart);
        systemObj.add("parts", sysParts);
        root.add("system_instruction", systemObj);

        JsonArray contents = new JsonArray();
        JsonObject userObj = new JsonObject();
        userObj.addProperty("role", "user");
        JsonArray userParts = new JsonArray();
        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", userMessage);
        userParts.add(userPart);
        userObj.add("parts", userParts);
        contents.add(userObj);
        root.add("contents", contents);

        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("maxOutputTokens", 100);
        genConfig.addProperty("temperature", 0.8);
        root.add("generationConfig", genConfig);

        return root.toString();
    }

    private static String parseResponse(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            return json.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString()
                    .trim();
        } catch (Exception e) {
            return null;
        }
    }
}
