package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.stand.Stand;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Перед первым классом с тегом ждёт готовности стенда: прогон, начатый вместе с подъёмом стенда, не падает на первом
 * запросе, а при недоступном стенде падает одной понятной ошибкой вместо таймаута Playwright. Классы без тега стенд не
 * ждут, чтобы не зависеть от него. Флаг, тег и стенд приходят в конструктор, чтобы проба подставляла свои через
 * обёртку, не трогая system properties соседних тестов.
 */
public final class StandReadinessExtension implements SuiteExtension {

  private final boolean check;
  private final String tag;
  private final Stand stand;

  public StandReadinessExtension() {
    this(
        TestConfig.standCheck(),
        "ui",
        new Stand(
            TestConfig.dbJdbcUrl(), TestConfig.authUrl(), TestConfig.apiUrl(), TestConfig.frontUrl(),
            TestConfig.standTimeout()
        )
    );
  }

  public StandReadinessExtension(boolean check, String tag, Stand stand) {
    this.check = check;
    this.tag = tag;
    this.stand = stand;
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    if (check && context.getTags().contains(tag)) {
      SuiteExtension.super.beforeAll(context);
    }
  }

  @Override
  public void beforeSuite(ExtensionContext context) {
    stand.await();
  }
}
