package io.student.rangiffler.config;

import java.util.Locale;

/**
 * Значение ищется по порядку: system property {@code rfr.<key>}, переменная окружения {@code RFR_<KEY>}, default.
 * Например, {@code front.url} → {@code -Drfr.front.url} / {@code RFR_FRONT_URL}.
 */
public final class Config {

  private static final String PROPERTY_PREFIX = "rfr.";
  private static final String ENV_PREFIX = "RFR_";

  private Config() {
  }

  public static String frontUrl() {
    return get("front.url", "http://localhost:3001");
  }

  public static String authUrl() {
    return get("auth.url", "http://localhost:9001");
  }

  public static String authJdbcUrl() {
    return get("auth.jdbc.url", "jdbc:mysql://localhost:3306/rangiffler-auth?serverTimezone=UTC");
  }

  public static String apiJdbcUrl() {
    return get("api.jdbc.url", "jdbc:mysql://localhost:3306/rangiffler-api?serverTimezone=UTC");
  }

  public static String dbUsername() {
    return get("db.username", "root");
  }

  public static String dbPassword() {
    return get("db.password", "secret");
  }

  public static boolean headless() {
    return Boolean.parseBoolean(get("headless", "true"));
  }

  private static String get(String key, String defaultValue) {
    String property = System.getProperty(PROPERTY_PREFIX + key);
    if (property != null && !property.isBlank()) {
      return property;
    }
    String env = System.getenv(ENV_PREFIX + key.toUpperCase(Locale.ROOT).replaceAll("[.-]", "_"));
    if (env != null && !env.isBlank()) {
      return env;
    }
    return defaultValue;
  }
}
