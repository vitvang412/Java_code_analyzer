package com.codeanalyzer.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpUtil {

    private static final int CONNECT_TIMEOUT = 15_000;
    private static final int READ_TIMEOUT    = 30_000;

    private HttpUtil() {}

    /** Simple GET – returns body as String */
    public static String get(String urlStr) throws Exception {
        HttpURLConnection conn = openConnection(urlStr);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        conn.setRequestProperty("Accept", "application/json, text/plain, */*");
        return readResponse(conn);
    }

    /** POST with JSON body – returns body as String */
    public static String post(String urlStr, String jsonBody) throws Exception {
        HttpURLConnection conn = openConnection(urlStr);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");

        byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
        }
        return readResponse(conn);
    }

    private static HttpURLConnection openConnection(String urlStr) throws Exception {
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        return conn;
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        BufferedReader reader;
        if (code >= 200 && code < 300) {
            reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            if (conn.getErrorStream() != null) {
                reader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            } else {
                throw new RuntimeException("HTTP " + code + " – no body");
            }
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append('\n');
        reader.close();
        if (code < 200 || code >= 300) {
            throw new RuntimeException("HTTP " + code + ": " + sb);
        }
        return sb.toString().trim();
    }
}
