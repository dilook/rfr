package io.student.rangiffler.data.fixture;

import io.student.rangiffler.data.TestUser;

/**
 * Тестовый пользователь, которого фикстура готовит в create() и освобождает в close(): создаёт и удаляет в БД или берёт из пула и возвращает в него.
 */
public interface UserFixture extends AutoCloseable {

  TestUser user();

  void create();

  @Override
  void close();
}
