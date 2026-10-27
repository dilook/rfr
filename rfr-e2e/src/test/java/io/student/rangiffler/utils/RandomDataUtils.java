package io.student.rangiffler.utils;

import java.util.UUID;

public final class RandomDataUtils {

  private RandomDataUtils() {
  }

  // rfr-auth: username 3..50 символов, password 3..12
  public static String randomUsername() {
    return "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }

  public static String randomPassword() {
    return "p" + UUID.randomUUID().toString().replace("-", "").substring(0, 9);
  }
}
