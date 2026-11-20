package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.condition.ProbeTargetCondition;
import io.student.rangiffler.jupiter.extension.SuiteExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Проверяет SuiteExtension как чёрный ящик: образцы-реализации пишут свои вызовы в журнал, а тест сверяет журнал
 * с семантикой extension'а уровня всего прогона.
 */
@Execution(ExecutionMode.SAME_THREAD)
class SuiteExtensionTest {

  private static final Queue<String> JOURNAL = new ConcurrentLinkedQueue<>();

  @BeforeEach
  void clearJournal() {
    JOURNAL.clear();
  }

  @Test
  @DisplayName("В параллельном прогоне трёх классов beforeSuite вызывается ровно один раз")
  void beforeSuiteShouldRunOnceForSeveralParallelClasses() {
    run(FirstJournalSample.class, SecondJournalSample.class, ThirdJournalSample.class);
    assertEquals(1L, count("before:JournalSuite"), "beforeSuite вызван не ровно один раз на три класса");
  }

  @Test
  @DisplayName("В параллельном прогоне трёх классов afterSuite вызывается ровно один раз")
  void afterSuiteShouldRunOnceForSeveralParallelClasses() {
    run(FirstJournalSample.class, SecondJournalSample.class, ThirdJournalSample.class);
    assertEquals(1L, count("after:JournalSuite"), "afterSuite вызван не ровно один раз на три класса");
  }

  @Test
  @DisplayName("beforeSuite вызывается до первого теста прогона")
  void beforeSuiteShouldRunBeforeFirstTest() {
    run(FirstJournalSample.class, SecondJournalSample.class, ThirdJournalSample.class);
    assertEquals("before:JournalSuite", JOURNAL.peek(), "Тест прогона выполнился раньше beforeSuite");
  }

  @Test
  @DisplayName("afterSuite вызывается после последнего теста прогона")
  void afterSuiteShouldRunAfterLastTest() {
    run(FirstJournalSample.class, SecondJournalSample.class, ThirdJournalSample.class);
    assertEquals("after:JournalSuite", List.copyOf(JOURNAL).getLast(), "Тест прогона выполнился позже afterSuite");
  }

  @Test
  @DisplayName("Две разные реализации SuiteExtension получают по одному своему beforeSuite")
  void differentSuiteExtensionsShouldEachRunOnce() {
    run(TwoSuitesSample.class, FirstJournalSample.class);
    assertEquals(
        List.of(1L, 1L),
        List.of(count("before:JournalSuite"), count("before:OtherJournalSuite")),
        "Разные реализации SuiteExtension получили не по одному beforeSuite"
    );
  }

  @Test
  @DisplayName("afterSuite вызывается после упавшего теста")
  void afterSuiteShouldRunAfterFailedTest() {
    run(FailedTestSample.class, FirstJournalSample.class);
    assertEquals(1L, count("after:JournalSuite"), "afterSuite не вызван ровно один раз после упавшего теста");
  }

  @Test
  @DisplayName("Упавший beforeSuite не вызывается повторно для следующих классов")
  void failedBeforeSuiteShouldNotRepeat() {
    run(FirstBrokenSetupSample.class, SecondBrokenSetupSample.class);
    assertEquals(1L, count("before:FailingBeforeSuite"), "Упавший beforeSuite вызван не ровно один раз на два класса");
  }

  @Test
  @DisplayName("Упавший beforeSuite не отменяет afterSuite")
  void afterSuiteShouldRunAfterFailedBeforeSuite() {
    run(FirstBrokenSetupSample.class, SecondBrokenSetupSample.class);
    assertEquals(1L, count("after:FailingBeforeSuite"), "afterSuite не вызван ровно один раз после упавшего beforeSuite");
  }

  @Test
  @DisplayName("После упавшего beforeSuite каждый класс падает и ни один тест не выполняется")
  void everyClassShouldFailAfterFailedBeforeSuite() {
    EngineExecutionResults results = run(FirstBrokenSetupSample.class, SecondBrokenSetupSample.class);
    assertEquals(
        List.of(2L, 0L),
        List.of(results.containerEvents().failed().count(), count("test")),
        "После упавшего beforeSuite класс не упал или тест выполнился без подготовки"
    );
  }

  @Test
  @DisplayName("Каждый класс после упавшего beforeSuite падает с исходным исключением в причине")
  void everyClassFailureShouldCarryFailedBeforeSuiteAsCause() {
    EngineExecutionResults results = run(FirstBrokenSetupSample.class, SecondBrokenSetupSample.class);
    assertEquals(
        List.of("beforeSuite образца FailingBeforeSuite падает намеренно", "beforeSuite образца FailingBeforeSuite падает намеренно"),
        results.containerEvents().failed().stream()
            .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow().getCause())
            .map(cause -> Optional.ofNullable(cause).map(Throwable::getMessage).orElse("без причины"))
            .toList(),
        "Падение класса после упавшего beforeSuite не несёт исходное исключение в причине"
    );
  }

  @Test
  @DisplayName("Упавший afterSuite несёт исключение упавшего beforeSuite как suppressed")
  void failedAfterSuiteShouldCarryFailedBeforeSuiteAsSuppressed() {
    EngineExecutionResults results = run(BrokenSuiteSample.class);
    assertTrue(
        results.containerEvents().failed().stream()
            .filter(event -> event.getTestDescriptor().getParent().isEmpty())
            .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow())
            .flatMap(failure -> Stream.iterate(failure, Objects::nonNull, Throwable::getCause))
            .flatMap(failure -> Arrays.stream(failure.getSuppressed()))
            .map(Throwable::getMessage)
            .anyMatch("beforeSuite образца BrokenSuite падает намеренно"::equals),
        "Исключение упавшего beforeSuite потеряно после упавшего afterSuite"
    );
  }

  @Test
  @DisplayName("Упавший afterSuite роняет прогон, а не проходит молча")
  void failedAfterSuiteShouldFailRun() {
    EngineExecutionResults results = run(BrokenTeardownSample.class);
    assertNotEquals(0L, results.containerEvents().failed().count(), "Падение afterSuite прошло молча");
  }

  @Test
  @DisplayName("Упавший afterSuite одной реализации не отменяет afterSuite другой")
  void failedAfterSuiteShouldNotSkipOtherAfterSuite() {
    run(BrokenTeardownSample.class);
    assertEquals(1L, count("after:JournalSuite"), "afterSuite другой реализации не вызван после упавшего afterSuite");
  }

  @Test
  @DisplayName("afterSuite получает корневой контекст прогона, тот же, что beforeSuite")
  void afterSuiteShouldReceiveRootContextOfBeforeSuite() {
    run(ContextSample.class, FirstJournalSample.class);
    assertEquals(
        List.of("before:[engine:junit-jupiter]", "after:[engine:junit-jupiter]"),
        JOURNAL.stream().filter(entry -> entry.contains("[engine:")).toList(),
        "afterSuite получил не корневой контекст прогона или не тот, что beforeSuite"
    );
  }

  private static EngineExecutionResults run(Class<?>... samples) {
    return EngineTestKit.engine("junit-jupiter")
        .selectors(Arrays.stream(samples).map(DiscoverySelectors::selectClass).toList())
        .configurationParameter(ProbeTargetCondition.ENABLED, "true")
        // Автоопределение подключило бы к образцам AllureJupiter и Stand*Extension из ServiceLoader.
        .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
        .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")
        .configurationParameter("junit.jupiter.execution.parallel.mode.default", "concurrent")
        .configurationParameter("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
        .execute();
  }

  private static long count(String entry) {
    return JOURNAL.stream().filter(entry::equals).count();
  }

  static final class JournalSuite implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Ожидание в beforeSuite образца JournalSuite прервано", e);
      }
      // Запись после паузы: если классы не ждут окончания beforeSuite, первой в журнале окажется запись теста.
      JOURNAL.add("before:JournalSuite");
    }

    @Override
    public void afterSuite(ExtensionContext context) {
      JOURNAL.add("after:JournalSuite");
    }
  }

  static final class OtherJournalSuite implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
      JOURNAL.add("before:OtherJournalSuite");
    }

    @Override
    public void afterSuite(ExtensionContext context) {
      JOURNAL.add("after:OtherJournalSuite");
    }
  }

  static final class FailingBeforeSuite implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
      JOURNAL.add("before:FailingBeforeSuite");
      throw new IllegalStateException("beforeSuite образца FailingBeforeSuite падает намеренно");
    }

    @Override
    public void afterSuite(ExtensionContext context) {
      JOURNAL.add("after:FailingBeforeSuite");
    }
  }

  static final class FailingAfterSuite implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
      JOURNAL.add("before:FailingAfterSuite");
    }

    @Override
    public void afterSuite(ExtensionContext context) {
      JOURNAL.add("after:FailingAfterSuite");
      throw new IllegalStateException("afterSuite образца FailingAfterSuite падает намеренно");
    }
  }

  static final class BrokenSuite implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
      throw new IllegalStateException("beforeSuite образца BrokenSuite падает намеренно");
    }

    @Override
    public void afterSuite(ExtensionContext context) {
      throw new IllegalStateException("afterSuite образца BrokenSuite падает намеренно");
    }
  }

  static final class ContextSuite implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
      JOURNAL.add("before:" + context.getUniqueId());
    }

    @Override
    public void afterSuite(ExtensionContext context) {
      JOURNAL.add("after:" + context.getUniqueId());
    }
  }

  @ProbeTarget
  @ExtendWith(ContextSuite.class)
  static class ContextSample {

    @Test
    void test() {
      JOURNAL.add("test");
    }
  }

  @ProbeTarget
  @ExtendWith(JournalSuite.class)
  static class FirstJournalSample {

    @Test
    void test() {
      JOURNAL.add("test");
    }
  }

  @ProbeTarget
  @ExtendWith(JournalSuite.class)
  static class SecondJournalSample {

    @Test
    void test() {
      JOURNAL.add("test");
    }
  }

  @ProbeTarget
  @ExtendWith(JournalSuite.class)
  static class ThirdJournalSample {

    @Test
    void test() {
      JOURNAL.add("test");
    }
  }

  @ProbeTarget
  @ExtendWith({JournalSuite.class, OtherJournalSuite.class})
  static class TwoSuitesSample {

    @Test
    void test() {
      JOURNAL.add("test");
    }
  }

  @ProbeTarget
  @ExtendWith(JournalSuite.class)
  static class FailedTestSample {

    @Test
    void test() {
      JOURNAL.add("test");
      fail("Образец падает намеренно");
    }
  }

  @ProbeTarget
  @ExtendWith(FailingBeforeSuite.class)
  static class FirstBrokenSetupSample {

    @Test
    void test() {
      JOURNAL.add("test");
    }
  }

  @ProbeTarget
  @ExtendWith(FailingBeforeSuite.class)
  static class SecondBrokenSetupSample {

    @Test
    void test() {
      JOURNAL.add("test");
    }
  }

  @ProbeTarget
  @ExtendWith(BrokenSuite.class)
  static class BrokenSuiteSample {

    @Test
    void test() {
      JOURNAL.add("test");
    }
  }

  // FailingAfterSuite подключён последним: Store закрывает его первым, и проба видит, закроется ли после него JournalSuite.
  @ProbeTarget
  @ExtendWith({JournalSuite.class, FailingAfterSuite.class})
  static class BrokenTeardownSample {

    @Test
    void test() {
      JOURNAL.add("test");
    }
  }
}
