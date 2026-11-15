package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.BrowserContext;
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
 * Трасса пишется для каждого контекста браузера UI-теста, а сохраняется только у упавших и прерванных. У прошедшего
 * теста она не останавливается: Playwright закрывает контекст, и незавершённая трасса отбрасывается вместе с ним, а
 * явная остановка лишь добавила бы точку отказа, которая красит прошедший тест. Контекст теста берётся так же, как в
 * BrowserTimeoutExtension; Playwright закрывает его в колбэках TestWatcher, которые JUnit
 * вызывает после afterEach, поэтому трасса останавливается на ещё живом контексте. Трассы контекстов сессий запускает и
 * останавливает SessionExtension, потому что только он знает, когда эти контексты создаются и какие параметры их
 * получили, а хранение и сохранение остаются здесь, чтобы он не зависел от устройства Store. Начатая трассировка
 * кладётся в Store теста под меткой: если предыдущий колбэк упал и трассировка не началась, остановка не создаёт
 * контекст заново и не добавляет своих ошибок к исходной. Метки контекста теста и сессий не пересекаются при любом
 * порядке параметров и разводят файлы и вложения одного теста. Каталог трасс сначала ищется в параметрах конфигурации
 * JUnit, чтобы пробы задавали временный, затем в TestConfig. Хеш уникального идентификатора в имени файла разводит
 * вызовы повторяемых и параметризованных тестов, которые при параллельном прогоне иначе писали бы в один файл.
 */
public final class TraceExtension implements BeforeEachCallback, AfterEachCallback {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(TraceExtension.class);

  @Override
  public void beforeEach(ExtensionContext context) {
    startTraceAndSaveToStore(context, "test", BrowserContextExtension.getOrCreateBrowserContext(context));
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) throws IOException {
    stopTraceAndAttachToReport(extensionContext, "test");
  }

  public static void startTraceAndSaveToStore(ExtensionContext extensionContext, String label, BrowserContext browserContext) {
    Tracing tracing = browserContext.tracing();
    tracing.start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
    extensionContext.getStore(NAMESPACE).put(label, tracing);
  }

  public static void stopTraceAndAttachToReport(ExtensionContext context, String label) throws IOException {
    Optional<Tracing> started = Optional.ofNullable(context.getStore(NAMESPACE).remove(label, Tracing.class));
    if (started.isEmpty() || context.getExecutionException().isEmpty()) {
      return;
    }
    Path tracePath = Path.of(context.getConfigurationParameter("rfr.trace.dir").orElseGet(TestConfig::traceDir)).resolve(
        "%s.%s-%08x-%s.zip".formatted(
            context.getRequiredTestClass().getName().replace('$', '.'),
            context.getRequiredTestMethod().getName(),
            context.getUniqueId().hashCode(),
            label
        )
    );
    Files.createDirectories(tracePath.getParent());
    started.get().stop(new Tracing.StopOptions().setPath(tracePath));
    try (InputStream content = Files.newInputStream(tracePath)) {
      Allure.attachment(label + ".zip", "application/zip", content, AttachmentOptions.withFileExtension(".zip"));
    }
  }
}
