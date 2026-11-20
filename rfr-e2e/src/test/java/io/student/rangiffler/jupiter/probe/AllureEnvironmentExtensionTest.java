package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.condition.ProbeTargetCondition;
import io.student.rangiffler.jupiter.extension.AllureEnvironmentExtension;
import io.student.rangiffler.jupiter.extension.SuiteExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет AllureEnvironmentExtension через EngineTestKit: образец гоняется с каталогом результатов Allure во временном
 * каталоге, а MySQL для версий схем — настоящая локальная, как в остальных пробах.
 */
class AllureEnvironmentExtensionTest {

  @Test
  @DisplayName("После прогона во временном каталоге результатов есть environment.properties со всеми ключами")
  void environmentShouldHaveAllKeysAfterRun(@TempDir Path results) throws IOException {
    run(results, EnvironmentSample.class);
    assertEquals(
        List.of("apiSchemaVersion", "apiUrl", "authSchemaVersion", "authUrl", "browser", "commit", "frontUrl", "headless"),
        keys(results.resolve("environment.properties")),
        "В environment.properties не все ключи окружения или есть лишние"
    );
  }

  @Test
  @DisplayName("environment.properties пишется, даже если beforeSuite следующей реализации упал")
  void environmentShouldBeWrittenAfterFailedBeforeSuite(@TempDir Path results) {
    run(results, BrokenSuiteSample.class);
    assertTrue(
        Files.exists(results.resolve("environment.properties")),
        "environment.properties не записан после упавшего beforeSuite другой реализации"
    );
  }

  @Test
  @DisplayName("Прогон без UI-классов не обращается к MySQL — версии схем в environment.properties равны none")
  void environmentShouldNotReadSchemaVersionsWithoutUiClasses(@TempDir Path results) throws IOException {
    run(results, EnvironmentSample.class);
    assertEquals(List.of("none", "none"), versions(results), "Прогон без UI-классов читал версии схем из MySQL");
  }

  @Test
  @DisplayName("Прогон с UI-классом читает версии схем — в environment.properties нет none")
  void environmentShouldReadSchemaVersionsWithUiClass(@TempDir Path results) throws IOException {
    run(results, UiSample.class);
    assertFalse(
        versions(results).contains("none"),
        "Прогон с UI-классом не читал версии схем из MySQL"
    );
  }

  @Test
  @DisplayName("Класс без тега после UI-класса не отменяет чтение версий схем")
  void untaggedClassShouldNotCancelSchemaVersionsAfterUiClass(@TempDir Path results) throws IOException {
    run(results, UiSample.class, EnvironmentSample.class);
    assertFalse(
        versions(results).contains("none"),
        "Класс без тега, выполненный после UI-класса, отменил чтение версий схем"
    );
  }

  // Без неявных параметров EngineTestKit выполняет классы последовательно в порядке селекторов.
  private static void run(Path results, Class<?>... samples) {
    EngineTestKit.engine("junit-jupiter")
        .selectors(Arrays.stream(samples).map(DiscoverySelectors::selectClass).toArray(DiscoverySelector[]::new))
        .configurationParameter(ProbeTargetCondition.ENABLED, "true")
        // Автоопределение подключило бы к образцам AllureJupiter и Stand*Extension из ServiceLoader.
        .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
        .configurationParameter("allure.results.directory", results.toString())
        .execute();
  }

  private static List<String> keys(Path file) throws IOException {
    return properties(file).stringPropertyNames().stream().sorted().toList();
  }

  private static List<String> versions(Path results) throws IOException {
    Properties environment = properties(results.resolve("environment.properties"));
    return List.of(environment.getProperty("authSchemaVersion"), environment.getProperty("apiSchemaVersion"));
  }

  private static Properties properties(Path file) throws IOException {
    Properties properties = new Properties();
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  static final class FailingSuite implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
      throw new IllegalStateException("beforeSuite образца FailingSuite падает намеренно");
    }
  }

  // Без тега ui: прогон из одного этого класса — не UI-прогон.
  @ProbeTarget
  @ExtendWith(AllureEnvironmentExtension.class)
  static class EnvironmentSample {

    @Test
    void test() {
    }
  }

  @ProbeTarget
  @Tag("ui")
  @ExtendWith(AllureEnvironmentExtension.class)
  static class UiSample {

    @Test
    void test() {
    }
  }

  // Порядок как в META-INF/services: AllureEnvironmentExtension раньше той, чей beforeSuite падает.
  @ProbeTarget
  @ExtendWith({AllureEnvironmentExtension.class, FailingSuite.class})
  static class BrokenSuiteSample {

    @Test
    void test() {
    }
  }
}
