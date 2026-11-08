package io.student.rangiffler.data;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Шаги удаления тестовых данных: выполняются все, даже если часть упала, первая ошибка несёт остальные как suppressed.
 */
final class TearDown implements Runnable {

  private final List<Runnable> steps;

  TearDown() {
    this(List.of());
  }

  private TearDown(List<Runnable> steps) {
    this.steps = steps;
  }

  TearDown with(Runnable step) {
    return new TearDown(Stream.concat(steps.stream(), Stream.of(step)).toList());
  }

  <T> TearDown withEach(List<T> items, Consumer<T> step) {
    return new TearDown(Stream.concat(steps.stream(), items.stream().<Runnable>map(item -> () -> step.accept(item))).toList());
  }

  @Override
  public void run() {
    RuntimeException failure = null;
    for (Runnable step : steps) {
      try {
        step.run();
      } catch (RuntimeException e) {
        if (failure == null) {
          failure = e;
        } else {
          failure.addSuppressed(e);
        }
      }
    }
    if (failure != null) {
      throw failure;
    }
  }
}
