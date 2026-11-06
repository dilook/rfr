package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.impl.junit.BrowserContextExtension;
import io.student.rangiffler.config.TestConfig;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Переходы получают отдельный тайм-аут, потому что идут через редиректы OAuth. В Options из @UsePlaywright тайм-аутов
 * нет, поэтому они задаются на контексте браузера, который Playwright создаёт для теста и держит в ThreadLocal: Page
 * теста создаётся из того же контекста и наследует их. BrowserContextExtension лежит во внутреннем пакете Playwright и
 * может измениться при обновлении библиотеки.
 */
public final class BrowserTimeoutExtension implements BeforeEachCallback {

  @Override
  public void beforeEach(ExtensionContext test) {
    BrowserContext context = BrowserContextExtension.getOrCreateBrowserContext(test);
    context.setDefaultTimeout(TestConfig.actionTimeout().toMillis());
    context.setDefaultNavigationTimeout(TestConfig.navigationTimeout().toMillis());
  }
}
