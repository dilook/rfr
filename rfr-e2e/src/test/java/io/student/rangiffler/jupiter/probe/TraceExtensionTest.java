package io.student.rangiffler.jupiter.probe;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.condition.ProbeTargetCondition;
import io.student.rangiffler.jupiter.extension.PlaywrightOptions;
import io.student.rangiffler.jupiter.extension.TraceExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Образцы запускают настоящий Chromium без стенда: страница наполняется через setContent, а каталог трасс уводится во
 * временный через параметр конфигурации rfr.trace.dir. Playwright читает @UsePlaywright с самого внешнего класса теста,
 * поэтому аннотация стоит и на пробе, а не только на вложенных образцах.
 */
@UsePlaywright(PlaywrightOptions.class)
class TraceExtensionTest {

  @Test
  @DisplayName("Трасса упавшего теста сохраняется в zip с полным именем класса, методом и хешем вызова")
  void traceShouldBeSavedForFailedTest(@TempDir Path traces) throws IOException {
    run(FailedSample.class, traces);
    try (Stream<Path> files = Files.list(traces)) {
      assertTrue(
          files.map(file -> file.getFileName().toString()).anyMatch(
              name -> name.matches("\\Q" + FailedSample.class.getName().replace('$', '.') + ".test-\\E[0-9a-f]{8}\\.zip")
          ),
          "Трасса упавшего теста не сохранена под именем класса, метода и хеша вызова"
      );
    }
  }

  @Test
  @DisplayName("Если трассировка не началась из-за упавшего beforeEach, afterEach не добавляет своих ошибок")
  void afterEachShouldNotFailWhenTracingDidNotStart(@TempDir Path traces) {
    assertEquals(
        0,
        run(BrokenBeforeEachSample.class, traces).testEvents().failed().stream()
            .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow())
            .mapToInt(error -> error.getSuppressed().length)
            .sum(),
        "afterEach добавил свою ошибку к ошибке упавшего beforeEach"
    );
  }

  @Test
  @DisplayName("Трасса прошедшего теста не сохраняется")
  void traceShouldNotBeSavedForPassedTest(@TempDir Path traces) throws IOException {
    run(PassedSample.class, traces);
    try (Stream<Path> files = Files.list(traces)) {
      assertEquals(0L, files.count(), "Трасса прошедшего теста сохранена, хотя не нужна");
    }
  }

  @Test
  @DisplayName("Трассировка не роняет прошедший тест")
  void tracingShouldNotFailPassedTest(@TempDir Path traces) {
    assertEquals(
        1L,
        run(PassedSample.class, traces).testEvents().succeeded().count(),
        "Прошедший образец упал из-за трассировки"
    );
  }

  @Test
  @DisplayName("Прошедший тест остаётся зелёным, даже если его контекст закрылся до конца трассировки")
  void passedTestShouldStayGreenWhenContextClosedEarly(@TempDir Path traces) {
    assertEquals(
        1L,
        run(ClosedContextSample.class, traces).testEvents().succeeded().count(),
        "Прошедший тест покраснел из-за остановки трассы на закрытом контексте"
    );
  }

  private static EngineExecutionResults run(Class<?> sample, Path traces) {
    return EngineTestKit.engine("junit-jupiter")
        .selectors(DiscoverySelectors.selectClass(sample))
        .configurationParameter(ProbeTargetCondition.ENABLED, "true")
        // Автоопределение подключило бы к образцам AllureJupiter и Stand*Extension из ServiceLoader.
        .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
        .configurationParameter("rfr.trace.dir", traces.toString())
        .execute();
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  @ExtendWith(TraceExtension.class)
  static class FailedSample {

    @Test
    void test(Page page) {
      page.setContent("<button>Probe</button>");
      fail("Образец падает намеренно");
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  @ExtendWith({BrokenBeforeEach.class, TraceExtension.class})
  static class BrokenBeforeEachSample {

    @Test
    void test(Page page) {
    }
  }

  static final class BrokenBeforeEach implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
      throw new IllegalStateException("beforeEach образца падает намеренно");
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  @ExtendWith(TraceExtension.class)
  static class ClosedContextSample {

    @Test
    void test(Page page) {
      page.setContent("<button>Probe</button>");
      page.context().close();
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  @ExtendWith(TraceExtension.class)
  static class PassedSample {

    @Test
    void test(Page page) {
      page.setContent("<button>Probe</button>");
    }
  }
}
