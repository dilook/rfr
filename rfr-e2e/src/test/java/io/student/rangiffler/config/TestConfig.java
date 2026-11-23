package io.student.rangiffler.config;

import java.time.Duration;
import java.util.Locale;

/**
 * Значение ищется по порядку: system property {@code rfr.<key>}, переменная окружения {@code RFR_<KEY>}, default.
 * Например, {@code front.url} → {@code -Drfr.front.url} / {@code RFR_FRONT_URL}.
 */
public final class TestConfig {

  private static final String PROPERTY_PREFIX = "rfr.";
  private static final String ENV_PREFIX = "RFR_";

  private TestConfig() {
  }

  public static String frontUrl() {
    return get("front.url", "http://localhost:3001");
  }

  public static String authUrl() {
    return get("auth.url", "http://localhost:9001");
  }

  public static String apiUrl() {
    return get("api.url", "http://localhost:8081");
  }

  public static String authJdbcUrl() {
    return get("auth.jdbc.url", "jdbc:mysql://localhost:3306/rangiffler-auth?serverTimezone=UTC");
  }

  public static String apiJdbcUrl() {
    return get("api.jdbc.url", "jdbc:mysql://localhost:3306/rangiffler-api?serverTimezone=UTC");
  }

  public static String dbJdbcUrl() {
    return get("db.jdbc.url", "jdbc:mysql://localhost:3306/?serverTimezone=UTC");
  }

  public static boolean standSchemaCheck() {
    return Boolean.parseBoolean(get("stand.schema.check", "false"));
  }

  public static boolean standCheck() {
    return Boolean.parseBoolean(get("standCheck", "false"));
  }

  public static Duration standTimeout() {
    return Duration.ofSeconds(Long.parseLong(get("standTimeout", "180")));
  }

  public static String authMigrations() {
    return get("auth.migrations", "filesystem:../rfr-auth/src/main/resources/db/migration/rangiffler-auth");
  }

  public static String apiMigrations() {
    return get("api.migrations", "filesystem:../rfr-api/src/main/resources/db/migration/rangiffler-api");
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

  public static Duration actionTimeout() {
    return Duration.ofSeconds(Long.parseLong(get("action.timeout", "5")));
  }

  public static Duration navigationTimeout() {
    return Duration.ofSeconds(Long.parseLong(get("navigation.timeout", "30")));
  }

  public static int viewportWidth() {
    return Integer.parseInt(get("viewport.width", "1920"));
  }

  public static int viewportHeight() {
    return Integer.parseInt(get("viewport.height", "1080"));
  }

  public static String commit() {
    return get("commit", "unknown");
  }

  public static String traceDir() {
    return get("trace.dir", "build/traces");
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
