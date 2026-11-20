package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.junit.Options;
import io.student.rangiffler.config.PlaywrightOptions;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.data.clients.MigrationDbClient;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * После прогона пишет окружение в environment.properties каталога результатов Allure, чтобы по отчёту было видно, на
 * каком стенде, браузере, коммите и версиях схем шли тесты. Файл пишется и после упавшего beforeSuite другой
 * реализации, поэтому в META-INF/services extension стоит первым: JUnit не вызывает beforeAll следующих extension'ов
 * после упавшего, и стоящий позже не успел бы зарегистрировать свой afterSuite. Версии схем читаются из MySQL, только
 * если в прогоне был класс с тегом ui: прогон без UI-тестов, как :rfr-e2e:test, к стенду не обращается и пишет версию
 * none. Отметку об UI-классе кладёт в Store корня только класс с тегом, чтобы класс без тега, выполненный позже, её не
 * сбросил. Недоступная MySQL даёт версию unavailable, а не падение. Каталог читается из параметра JUnit
 * allure.results.directory: в Gradle это то же system property, что у Allure, а проба подставляет временный каталог без
 * глобального состояния.
 */
public final class AllureEnvironmentExtension implements SuiteExtension {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(AllureEnvironmentExtension.class);

  @Override
  public void beforeAll(ExtensionContext context) {
    ExtensionContext.Store store = context.getRoot().getStore(NAMESPACE);
    if (context.getTags().contains("ui")) {
      store.put("ui", true);
    }
    SuiteExtension.super.beforeAll(context);
  }

  @Override
  public void afterSuite(ExtensionContext context) {
    Path directory = Path.of(context.getConfigurationParameter("allure.results.directory").orElse("build/allure-results"));
    Options options = new PlaywrightOptions().getOptions();
    boolean suiteHasUiTest = Boolean.TRUE.equals(context.getRoot().getStore(NAMESPACE).get("ui", Boolean.class));
    String authSchemaVersion = suiteHasUiTest ?
        new MigrationDbClient(TestConfig.authJdbcUrl(), "rangiffler-auth", TestConfig.authMigrations()).version()
        : "none";
    String apiSchemaVersion = suiteHasUiTest ?
        new MigrationDbClient(TestConfig.apiJdbcUrl(), "rangiffler-api", TestConfig.apiMigrations()).version()
        : "none";
    List<String> lines = List.of(
        "frontUrl=" + TestConfig.frontUrl(),
        "authUrl=" + TestConfig.authUrl(),
        "apiUrl=" + TestConfig.apiUrl(),
        "browser=" + options.browserName,
        "headless=" + options.headless,
        "commit=" + TestConfig.commit(),
        "authSchemaVersion=" + authSchemaVersion,
        "apiSchemaVersion=" + apiSchemaVersion
    );
    try {
      Files.createDirectories(directory);
      Files.write(directory.resolve("environment.properties"), lines, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Не удалось записать environment.properties в каталог результатов Allure " + directory, e);
    }
  }
}
