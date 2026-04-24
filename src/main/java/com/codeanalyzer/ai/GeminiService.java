package com.codeanalyzer.ai;

import com.codeanalyzer.util.AppConfig;
import com.codeanalyzer.util.HttpUtil;
import com.codeanalyzer.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Low-level wrapper for the Gemini 1.5 Flash REST API.
 * Sends a prompt and returns the generated text.
 */
public class GeminiService {

    private static final AppConfig CFG = AppConfig.getInstance();

    /**
     * Sends a text prompt to Gemini and returns the model's reply.
     */
    public String generate(String prompt) throws Exception {
        String apiKey   = CFG.geminiApiKey();
        String model    = CFG.geminiModel();
        String baseUrl  = CFG.geminiApiUrl();

        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            throw new IllegalStateException("Gemini API key is not configured in config.properties");
        }

        String url = baseUrl + model + ":generateContent?key=" + apiKey;

        // Build request JSON
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(part);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject requestBody = new JsonObject();
        requestBody.add("contents", contents);

        // Temperature / safety config
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.3);
        genConfig.addProperty("maxOutputTokens", 1024);
        requestBody.add("generationConfig", genConfig);

        String responseJson = HttpUtil.post(url, requestBody.toString());

        // Parse: candidates[0].content.parts[0].text
        try {
            JsonObject resp = JsonUtil.parseObject(responseJson);
            JsonArray candidates = resp.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new RuntimeException("No candidates in Gemini response: " + responseJson);
            }
            JsonObject candidate = candidates.get(0).getAsJsonObject();
            JsonObject respContent = candidate.getAsJsonObject("content");
            JsonArray respParts   = respContent.getAsJsonArray("parts");
            return respParts.get(0).getAsJsonObject().get("text").getAsString().trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response: " + responseJson, e);
        }
    }
}
