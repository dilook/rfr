package io.student.rangiffler.jupiter.probe;

import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Вложенный прогон EngineTestKit в собственном потоке. PlaywrightExtension кэширует Playwright в ThreadLocal, а в конце
 * прогона закрывает его, не очищая ThreadLocal: второй прогон образца с @UsePlaywright в том же потоке получил бы
 * закрытый Playwright и упал с «Playwright connection closed». Это ошибка Playwright 1.63; когда её исправят, пробы
 * снова смогут вызывать execute() напрямую.
 */
final class IsolatedRun {

  private static final Duration TIMEOUT = Duration.ofMinutes(2);

  private final EngineTestKit.Builder run;

  IsolatedRun(EngineTestKit.Builder run) {
    this.run = run;
  }

  EngineExecutionResults results() {
    ExecutorService thread = Executors.newSingleThreadExecutor();
    Future<EngineExecutionResults> results = thread.submit(run::execute);
    try {
      return results.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Ожидание вложенного прогона EngineTestKit прервано", e);
    } catch (ExecutionException e) {
      throw new IllegalStateException("Вложенный прогон EngineTestKit упал", e.getCause());
    } catch (TimeoutException e) {
      results.cancel(true);
      throw new IllegalStateException("Вложенный прогон EngineTestKit не закончился за " + TIMEOUT, e);
    } finally {
      thread.shutdownNow();
    }
  }
}
