package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.condition.ProbeTargetCondition;
import io.student.rangiffler.jupiter.extension.AllureEnvironmentExtension;
import io.student.rangiffler.jupiter.extension.SuiteExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет AllureEnvironmentExtension через EngineTestKit: образец гоняется с каталогом результатов Allure во временном
 * каталоге, а MySQL для версий схем — настоящая локальная, как в остальных пробах.
 */
class AllureEnvironmentExtensionTest {

  @Test
  @DisplayName("После прогона во временном каталоге результатов есть environment.properties со всеми ключами")
  void environmentShouldHaveAllKeysAfterRun(@TempDir Path results) throws IOException {
    run(EnvironmentSample.class, results);
    assertEquals(
        List.of("apiSchemaVersion", "apiUrl", "authSchemaVersion", "authUrl", "browser", "commit", "frontUrl", "headless"),
        keys(results.resolve("environment.properties")),
        "В environment.properties не все ключи окружения или есть лишние"
    );
  }

  @Test
  @DisplayName("environment.properties пишется, даже если beforeSuite следующей реализации упал")
  void environmentShouldBeWrittenAfterFailedBeforeSuite(@TempDir Path results) {
    run(BrokenSuiteSample.class, results);
    assertTrue(
        Files.exists(results.resolve("environment.properties")),
        "environment.properties не записан после упавшего beforeSuite другой реализации"
    );
  }

  private static void run(Class<?> sample, Path results) {
    EngineTestKit.engine("junit-jupiter")
        .selectors(DiscoverySelectors.selectClass(sample))
        .configurationParameter(ProbeTargetCondition.ENABLED, "true")
        // Автоопределение подключило бы к образцам AllureJupiter и Stand*Extension из ServiceLoader.
        .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
        .configurationParameter("allure.results.directory", results.toString())
        .execute();
  }

  private static List<String> keys(Path file) throws IOException {
    Properties properties = new Properties();
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties.stringPropertyNames().stream().sorted().toList();
  }

  static final class FailingSuite implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
      throw new IllegalStateException("beforeSuite образца FailingSuite падает намеренно");
    }
  }

  @ProbeTarget
  @ExtendWith(AllureEnvironmentExtension.class)
  static class EnvironmentSample {

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
