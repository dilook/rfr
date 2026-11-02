package io.student.rangiffler.jupiter.extension;

import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Один прогон SuiteExtension: попадает в корневой Store до beforeSuite, выполняет его ровно один раз, помнит его
 * исключение для всех следующих классов и вызывает afterSuite с тем же корневым контекстом при закрытии Store, даже если
 * beforeSuite упал.
 */
final class SuiteRun implements AutoCloseable {

  private final SuiteExtension extension;
  private final ExtensionContext root;
  private final AtomicBoolean started;
  private final AtomicReference<Throwable> failure;

  SuiteRun(SuiteExtension extension, ExtensionContext root) {
    this(extension, root, new AtomicBoolean(), new AtomicReference<>());
  }

  SuiteRun(SuiteExtension extension, ExtensionContext root, AtomicBoolean started, AtomicReference<Throwable> failure) {
    this.extension = extension;
    this.root = root;
    this.started = started;
    this.failure = failure;
  }

  synchronized void start() {
    if (started.compareAndSet(false, true)) {
      try {
        extension.beforeSuite(root);
      } catch (Throwable e) {
        // AssertionError из beforeSuite тоже обязан уронить все классы, иначе следующие пойдут без подготовки.
        failure.set(e);
      }
    }
    if (failure.get() != null) {
      throw new IllegalStateException(
          "beforeSuite расширения " + extension.getClass().getName() + " упал, классы прогона не выполняются",
          failure.get()
      );
    }
  }

  @Override
  public void close() {
    try {
      extension.afterSuite(root);
    } catch (Throwable e) {
      if (failure.get() != null) {
        e.addSuppressed(failure.get());
      }
      throw e;
    }
  }
}
