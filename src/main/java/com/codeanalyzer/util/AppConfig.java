package com.codeanalyzer.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static AppConfig instance;
    private final Properties props = new Properties();

    private AppConfig() {
        try (InputStream is = AppConfig.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                System.err.println("[AppConfig] config.properties not found on classpath!");
            }
        } catch (IOException e) {
            System.err.println("[AppConfig] Error loading config: " + e.getMessage());
        }
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) instance = new AppConfig();
        return instance;
    }

    public String get(String key) {
        return props.getProperty(key, "");
    }

    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try { return Integer.parseInt(props.getProperty(key)); }
        catch (Exception e) { return defaultValue; }
    }

    public long getLong(String key, long defaultValue) {
        try { return Long.parseLong(props.getProperty(key)); }
        catch (Exception e) { return defaultValue; }
    }

    // ── Convenience shortcuts ──────────────────────────────

    public String dbUrl()           { return get("db.url"); }
    public String dbUsername()      { return get("db.username"); }
    public String dbPassword()      { return get("db.password"); }

    public String geminiApiKey()    { return get("gemini.api.key"); }
    public String geminiModel()     { return get("gemini.model", "gemini-1.5-flash"); }
    public String geminiApiUrl()    { return get("gemini.api.url"); }

    public String chromeProfilePath(){ return get("chrome.profile.path"); }
    public String chromeProfileDir() { return get("chrome.profile.dir", "Default"); }

    public long crawlerDelayMs()    { return getLong("crawler.delay.ms", 2000L); }
    public int crawlerScheduleHours(){ return getInt("crawler.schedule.hours", 24); }
}
