package io.student.rangiffler.data.fixture;

import io.student.rangiffler.data.TestUser;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Пользователь из общего пула заранее заведённых: берётся из очереди на время теста и возвращается в неё, в БД ничего не создаётся и не удаляется.
 */
public final class StaticUserFixture implements UserFixture {

  // Пул нужен там, где пользователей нельзя создать на лету; пары не заводятся в стенде, тесты их только читают.
  private static final BlockingQueue<TestUser> QUEUE = new LinkedBlockingQueue<>(List.of(
      new TestUser("static-pine", "12345"),
      new TestUser("static-tundra", "12345"),
      new TestUser("static-fjord", "12345")
  ));
  private static final long TIMEOUT_SECONDS = 5;

  private final AtomicReference<Optional<TestUser>> taken;

  public StaticUserFixture() {
    this(new AtomicReference<>(Optional.empty()));
  }

  private StaticUserFixture(AtomicReference<Optional<TestUser>> taken) {
    this.taken = taken;
  }

  @Override
  public TestUser user() {
    return taken.get().orElseThrow(
        () -> new IllegalStateException("Статический пользователь не взят из очереди: create() не вызывался или упал")
    );
  }

  @Override
  public void create() {
    try {
      TestUser user = QUEUE.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (user == null) {
        throw new IllegalStateException(
            "Очередь статических пользователей пуста дольше " + TIMEOUT_SECONDS + " с: все пользователи пула заняты другими тестами"
        );
      }
      taken.set(Optional.of(user));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Ожидание статического пользователя прервано", e);
    }
  }

  @Override
  public void close() {
    taken.getAndSet(Optional.empty()).ifPresent(QUEUE::add);
  }
}
