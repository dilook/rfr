package io.student.rangiffler.session.fixture;

/**
 * Сессия браузера, которую фикстура готовит в create() и освобождает в close(): тест получает только готовую session(),
 * а сама фикстура остаётся в Store и закрывается JUnit.
 */
public interface SessionFixture<S> extends AutoCloseable {

  S session();

  void create();

  @Override
  void close();
}
