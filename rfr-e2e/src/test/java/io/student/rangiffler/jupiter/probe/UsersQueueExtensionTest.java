package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.data.StaticUserFixture;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.annotation.StaticUser;
import io.student.rangiffler.jupiter.condition.ProbeTargetCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Execution(ExecutionMode.SAME_THREAD)
class UsersQueueExtensionTest {

  @Test
  @DisplayName("Четыре теста подряд на пуле из трёх статических пользователей проходят")
  void fourTestsInARowShouldPassOnPoolOfThree() {
    assertEquals(
        4L,
        run(FourStaticUserTests.class).testEvents().succeeded().count(),
        "Не все четыре теста подряд прошли на пуле из трёх статических пользователей"
    );
  }

  @Test
  @DisplayName("После упавшего теста статический пользователь возвращается в очередь")
  void userShouldReturnToQueueAfterFailedTest() {
    entry(FailedStaticUser.class);
    assertTrue(poolIsFull(), "Пользователь упавшего теста не вернулся в очередь");
  }

  @Test
  @DisplayName("При пустой очереди create() падает по таймауту с понятной ошибкой")
  void createShouldFailWithClearErrorOnEmptyQueue() {
    assertTrue(
        emptyQueueError().startsWith("Очередь статических пользователей пуста"),
        "При пустой очереди create() не упал с ошибкой о пустой очереди"
    );
  }

  @Test
  @DisplayName("Повторный close() не возвращает пользователя в очередь дважды")
  void secondCloseShouldNotReturnUserTwice() {
    UserFixture fixture = new StaticUserFixture();
    fixture.create();
    fixture.close();
    fixture.close();
    assertTrue(
        emptyQueueError().startsWith("Очередь статических пользователей пуста"),
        "После повторного close() в очереди больше трёх пользователей"
    );
  }

  @Test
  @DisplayName("Повторный create() не уводит пользователя из пула")
  void secondCreateShouldNotTakeUserFromPool() {
    UserFixture fixture = new StaticUserFixture();
    fixture.create();
    try {
      fixture.create();
    } catch (IllegalStateException expected) {
      // Отказ повторного create() допустим: проба проверяет только, что после close() пул снова полон
    } finally {
      fixture.close();
    }
    assertTrue(poolIsFull(), "Повторный create() увёл пользователя из пула");
  }

  private static String emptyQueueError() {
    List<UserFixture> pool = Stream.<UserFixture>generate(StaticUserFixture::new).limit(4).toList();
    try {
      pool.forEach(UserFixture::create);
      return "Из пула трёх пользователей взято четыре";
    } catch (IllegalStateException e) {
      return e.getMessage();
    } finally {
      pool.forEach(UserFixture::close);
    }
  }

  private static boolean poolIsFull() {
    List<UserFixture> pool = Stream.<UserFixture>generate(StaticUserFixture::new).limit(3).toList();
    try {
      pool.forEach(UserFixture::create);
      return true;
    } catch (IllegalStateException e) {
      return false;
    } finally {
      pool.forEach(UserFixture::close);
    }
  }

  private static EngineExecutionResults run(Class<?> sample) {
    return EngineTestKit.engine("junit-jupiter")
        .selectors(DiscoverySelectors.selectClass(sample))
        .configurationParameter(ProbeTargetCondition.ENABLED, "true")
        // Автоопределение подключило бы к образцам extension'ы из ServiceLoader (AllureJupiter, StandSchemaExtension).
        .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
        .execute();
  }

  private static Map<String, String> entry(Class<?> sample) {
    return run(sample).allEvents().reportingEntryPublished().stream()
        .map(event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Образец " + sample.getSimpleName() + " не опубликовал запись"));
  }

  @ProbeTarget
  @Execution(ExecutionMode.SAME_THREAD)
  static class FourStaticUserTests {

    @RepeatedTest(4)
    void test(@StaticUser TestUser user) {
    }
  }

  @ProbeTarget
  static class FailedStaticUser {

    @Test
    void test(@StaticUser TestUser user, TestReporter reporter) {
      reporter.publishEntry("username", user.username());
      fail("Образец падает намеренно");
    }
  }
}
