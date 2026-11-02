package io.student.rangiffler.jupiter.extension;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public interface SuiteExtension extends BeforeAllCallback {

  @Override
  default void beforeAll(ExtensionContext context) {
    context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL)
        .computeIfAbsent(getClass(), key -> new SuiteRun(this, context.getRoot()), SuiteRun.class)
        .start();
  }

  default void beforeSuite(ExtensionContext context) {
  }

  default void afterSuite(ExtensionContext context) {
  }
}
