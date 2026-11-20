package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;
import io.student.rangiffler.config.TestConfig;

/**
 * Размер окна задан явно: от высоты окна зависит ширина карты на главной, и раскладка не должна меняться между
 * headless и видимым браузером.
 */
public final class PlaywrightOptions implements OptionsFactory {

  @Override
  public Options getOptions() {
    return new Options()
        .setBrowserName("chromium")
        .setHeadless(TestConfig.headless())
        .setBaseUrl(TestConfig.frontUrl())
        .setTestIdAttribute("data-test-id")
        .setContextOptions(contextOptions());
  }

  private Browser.NewContextOptions contextOptions() {
    return new Browser.NewContextOptions()
        .setLocale("en-US")
        .setViewportSize(TestConfig.viewportWidth(), TestConfig.viewportHeight());
  }
}
