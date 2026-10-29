package io.student.rangiffler.data;

/**
 * Тестовый пользователь, которого фикстура создаёт в БД и удаляет после теста.
 */
public interface UserFixture extends AutoCloseable {

  TestUser user();

  void create();

  @Override
  void close();
}
