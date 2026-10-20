package io.student.rangiffler.config;

public final class Config {

  private Config() {
  }

  public static String frontUrl() {
    return System.getProperty("front.url", "http://localhost:3001");
  }

  public static boolean headless() {
    return Boolean.parseBoolean(System.getProperty("headless", "true"));
  }
}
