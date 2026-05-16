package com.codeanalyzer.ai;

import com.codeanalyzer.util.AppConfig;
import com.codeanalyzer.util.HttpUtil;
import com.codeanalyzer.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Low-level wrapper for the Gemini REST API (generateContent endpoint).
 * Sends a prompt and returns the generated text.
 *
 * Config keys (config.properties):
 *   gemini.api.key   – your Google AI Studio API key
 *   gemini.model     – e.g. gemini-1.5-flash
 *   gemini.api.url   – https://generativelanguage.googleapis.com/v1beta/models/
 */
public class GeminiService {

    private static final AppConfig CFG = AppConfig.getInstance();

    /**
     * Sends a text prompt to Gemini and returns the model's reply.
     *
     * @param prompt The prompt to send.
     * @return The generated text from Gemini.
     * @throws Exception if the API call fails or the key is not configured.
     */
    public String generate(String prompt) throws Exception {
        String apiKey  = CFG.geminiApiKey();
        String model   = CFG.geminiModel();
        String baseUrl = CFG.geminiApiUrl();

        if (apiKey == null || apiKey.isBlank()
                || apiKey.equals("YOUR_GEMINI_API_KEY_HERE")
                || apiKey.equals("CHANGE_ME")) {
            throw new IllegalStateException(
                    "Gemini API key is not configured. Please set GEMINI_API_KEY or 'gemini.api.key' in config.properties.");
        }

        String url = baseUrl + model + ":generateContent?key=" + apiKey;

        // Build request body
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

        // Generation config: conservative temperature, enough tokens for JSON output
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.2);
        
        // Use camelCase as required by Gson mapping, but remove responseMimeType 
        // because it is not strictly supported in all v1 model versions or requires 
        // a specific response_schema to be present.
        genConfig.addProperty("maxOutputTokens", 2048);
        
        requestBody.add("generationConfig", genConfig);

        String responseJson = HttpUtil.post(url, requestBody.toString());

        // Parse: candidates[0].content.parts[0].text
        try {
            JsonObject resp = JsonUtil.parseObject(responseJson);

            // Check for error response from the API
            if (resp.has("error")) {
                JsonObject error = resp.getAsJsonObject("error");
                String errMsg = error.has("message") ? error.get("message").getAsString() : responseJson;
                throw new RuntimeException("Gemini API error: " + errMsg);
            }

            JsonArray candidates = resp.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                // Check for promptFeedback (blocked by safety filter)
                if (resp.has("promptFeedback")) {
                    throw new RuntimeException("Content blocked by Gemini safety filters. Response: " + responseJson);
                }
                throw new RuntimeException("No candidates in Gemini response: " + responseJson);
            }

            JsonObject candidate   = candidates.get(0).getAsJsonObject();
            JsonObject respContent = candidate.getAsJsonObject("content");
            JsonArray  respParts   = respContent.getAsJsonArray("parts");
            String text = respParts.get(0).getAsJsonObject().get("text").getAsString().trim();

            // Strip markdown fences if Gemini wraps the JSON
            if (text.startsWith("```")) {
                text = text.replaceAll("^```(json)?\\s*", "").replaceAll("\\s*```$", "").trim();
            }
            return text;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response: " + responseJson, e);
        }
    }
}
