package io.student.rangiffler.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;

public class PlaywrightOptions implements OptionsFactory {

  @Override
  public Options getOptions() {
    return new Options()
        .setBrowserName("chromium")
        .setHeadless(Config.headless())
        .setBaseUrl(Config.frontUrl())
        .setContextOptions(new Browser.NewContextOptions().setLocale("en-US"));
  }
}
