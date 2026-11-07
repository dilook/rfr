package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.impl.junit.BrowserContextExtension;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.student.rangiffler.config.TestConfig;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Трасса пишется для каждого UI-теста, а сохраняется только у упавших и прерванных. У прошедшего теста она не
 * останавливается: Playwright закрывает контекст теста, и незавершённая трасса отбрасывается вместе с ним, а явная
 * остановка лишь добавила бы точку отказа, которая красит прошедший тест. Контекст берётся так же, как в
 * BrowserTimeoutExtension; Playwright закрывает его в колбэках TestWatcher, которые JUnit
 * вызывает после afterEach, поэтому трасса останавливается на ещё живом контексте. Начатая трассировка кладётся в Store
 * теста: если предыдущий beforeEach упал и трассировка не началась, afterEach не создаёт контекст заново и не добавляет
 * своих ошибок к исходной. Каталог трасс сначала ищется в параметрах конфигурации JUnit, чтобы пробы задавали временный,
 * затем в TestConfig. Хеш уникального идентификатора в имени файла разводит вызовы повторяемых и параметризованных
 * тестов, которые при параллельном прогоне иначе писали бы в один файл.
 */
public final class TraceExtension implements BeforeEachCallback, AfterEachCallback {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(TraceExtension.class);

  @Override
  public void beforeEach(ExtensionContext test) {
    Tracing tracing = BrowserContextExtension.getOrCreateBrowserContext(test).tracing();
    tracing.start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
    test.getStore(NAMESPACE).put(Tracing.class, tracing);
  }

  @Override
  public void afterEach(ExtensionContext test) throws IOException {
    Optional<Tracing> started = Optional.ofNullable(test.getStore(NAMESPACE).remove(Tracing.class, Tracing.class));
    if (started.isEmpty() || test.getExecutionException().isEmpty()) {
      return;
    }
    Path trace = Path.of(test.getConfigurationParameter("rfr.trace.dir").orElseGet(TestConfig::traceDir)).resolve(
        "%s.%s-%08x.zip".formatted(
            test.getRequiredTestClass().getName().replace('$', '.'),
            test.getRequiredTestMethod().getName(),
            test.getUniqueId().hashCode()
        )
    );
    Files.createDirectories(trace.getParent());
    started.get().stop(new Tracing.StopOptions().setPath(trace));
    try (InputStream content = Files.newInputStream(trace)) {
      Allure.attachment("trace.zip", "application/zip", content, AttachmentOptions.withFileExtension(".zip"));
    }
  }
}
