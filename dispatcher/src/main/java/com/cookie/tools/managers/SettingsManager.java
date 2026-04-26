package com.cookie.tools.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javafx.scene.control.Alert.AlertType;

public class SettingsManager {
    private static SettingsManager instance;
    private final Properties props = new Properties();
    
    // path dei salvataggi nel filesystem
    private static final String BASE_DIR = getBasePath();
    public static final String PACKETS_DIR  = BASE_DIR + "/saved_packet_templates";
    public static final String HEADERS_DIR  = BASE_DIR + "/saved_headers_templates";
    public static final String URLS_DIR     = BASE_DIR + "/saved_url_templates";
    public static final String SETTINGS_FILE = BASE_DIR + "/settings.properties";
    public static final String LOGS_DIR     = BASE_DIR + "/saved_logs";
    // chiavi
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_MAX_REQUESTS = "maxParallelRequests";
    private static final String KEY_LAST_URL = "lastUrl";

    // default
    private static final String DEFAULT_LANGUAGE = "en";
    private static final String DEFAULT_MAX_REQUESTS = "3";
    private static final String DEFAULT_LAST_URL = "";

    public static SettingsManager getInstance() {
        if (instance == null) instance = new SettingsManager();
        return instance;
    }

    private SettingsManager() {
        load();
    }

    // Detect dell'os
    private static String getBasePath() {
        String os = System.getProperty("os.name").toLowerCase();
        String dir;
        if (os.contains("win")) {
            dir = System.getenv("APPDATA") + "/hookshot";
        } else {
            dir = System.getProperty("user.home") + "/.config/hookshot";
        }
        // crea tutte le cartelle se non esistono
        new File(dir + "/saved_packet_templates").mkdirs();
        new File(dir + "/saved_headers_templates").mkdirs();
        new File(dir + "/saved_url_templates").mkdirs();
        new File(dir + "/saved_logs").mkdirs();
        return dir;
    }

    // --- LOAD / SAVE ---

    private void load() {
        try (FileInputStream fis = new FileInputStream(SETTINGS_FILE)) {
            props.load(fis);
        } catch (IOException e) {
            // file non esiste ancora, usiamo i default
            props.setProperty(KEY_LANGUAGE, DEFAULT_LANGUAGE);
            props.setProperty(KEY_MAX_REQUESTS, DEFAULT_MAX_REQUESTS);
            props.setProperty(KEY_LAST_URL, DEFAULT_LAST_URL);
        }
    }

    private void save() {
        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            props.store(fos, "HookShot Settings");
        } catch (IOException e) {
            SceneManager.getInstance().showAlert(
                LanguageManager.getInstance().getBundle().getString("alert.errorTitle"),
                LanguageManager.getInstance().getBundle().getString("alert.errorSaveSettings") + " " + e.getMessage(),
                AlertType.ERROR);
        }
    }

    // --- GETTER / SETTER ---

    public String getLanguage() {
        return props.getProperty(KEY_LANGUAGE, DEFAULT_LANGUAGE);
    }

    public void setLanguage(String language) {
        props.setProperty(KEY_LANGUAGE, language);
        save();
    }

    public int getMaxParallelRequests() {
        return Integer.parseInt(props.getProperty(KEY_MAX_REQUESTS, DEFAULT_MAX_REQUESTS));
    }

    public void setMaxParallelRequests(int max) {
        props.setProperty(KEY_MAX_REQUESTS, String.valueOf(max));
        save();
    }

    public String getLastUrl() {
        return props.getProperty(KEY_LAST_URL, DEFAULT_LAST_URL);
    }

    public void setLastUrl(String url) {
        props.setProperty(KEY_LAST_URL, url);
        save();
    }
}