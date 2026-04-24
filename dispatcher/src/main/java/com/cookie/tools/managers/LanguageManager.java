package com.cookie.tools.managers;

import java.util.Locale;
import java.util.ResourceBundle;

public class LanguageManager {
    private static LanguageManager instance;
    private ResourceBundle bundle;

    public enum SupportedLanguage {
        ENGLISH("en", "en-US"),
        ITALIAN("it", "it-IT");

        private final String code;
        private final String tag;

        SupportedLanguage(String code, String tag) {
            this.code = code;
            this.tag = tag;
        }

        public String getCode() { return code; }
        public String getTag() { return tag; }

        public static SupportedLanguage fromCode(String code) {
            for (SupportedLanguage l : values()) {
                if (l.code.equals(code)) return l;
            }
            return ENGLISH; // fallback
        }
    }

    public static LanguageManager getInstance() {
        if (instance == null) instance = new LanguageManager();
        return instance;
    }

    private LanguageManager() {
        // carica la lingua salvata nei settings
        load(SettingsManager.getInstance().getLanguage());
    }

    public void load(String langCode) {
        SupportedLanguage lang = SupportedLanguage.fromCode(langCode);
        Locale locale = Locale.forLanguageTag(lang.getTag());
        bundle = ResourceBundle.getBundle("com.cookie.tools.i18n.language", locale);
    }

    public ResourceBundle getBundle() { return bundle; }

    public SupportedLanguage getCurrentLanguage() {
        return SupportedLanguage.fromCode(SettingsManager.getInstance().getLanguage());
    }
}