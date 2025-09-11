package com.iseria.infra;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Properties PROPS = new Properties();
    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/market.properties")) {
            PROPS.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    public static String get(String key, String defaultValue) {
        return PROPS.getProperty(key, defaultValue);
    }
    public static int getInt(String key, int defaultValue) {
        return Integer.parseInt(get(key, String.valueOf(defaultValue)));
    }
}
