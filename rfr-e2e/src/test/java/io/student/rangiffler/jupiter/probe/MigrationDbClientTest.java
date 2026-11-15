package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.data.clients.TemporarySchemaDbClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет сверку схемы с миграциями бэкенда на временной схеме локальной MySQL, которую накатывает и удаляет
 * TemporarySchemaDbClient.
 */
class MigrationDbClientTest {

  @Test
  @DisplayName("Схема, совпадающая с миграциями, не даёт расхождения")
  void matchingSchemaShouldHaveNoDrift(@TempDir Path dir) {
    try (TemporarySchemaDbClient schema = new TemporarySchemaDbClient(dir)) {
      schema.write(1);
      schema.migrate();
      assertEquals(Optional.empty(), schema.migrations().drift(), "Совпадающая схема дала расхождение");
    }
  }

  @Test
  @DisplayName("Неприменённая миграция просит перезапустить бэкенды, не удаляя схему")
  void pendingMigrationShouldAskToRestartBackends(@TempDir Path dir) {
    try (TemporarySchemaDbClient schema = new TemporarySchemaDbClient(dir)) {
      schema.write(1);
      schema.migrate();
      schema.write(2);
      assertTrue(
          schema.migrations().drift().orElse("").contains("Flyway применит недостающие миграции"),
          "Неприменённая миграция не попросила перезапустить бэкенды"
      );
    }
  }

  @Test
  @DisplayName("Миграция из БД, которой нет в ветке, просит удалить схему")
  void futureMigrationShouldAskToDropSchema(@TempDir Path dir) {
    try (TemporarySchemaDbClient schema = new TemporarySchemaDbClient(dir)) {
      schema.write(1);
      schema.write(2);
      schema.migrate();
      schema.delete(2);
      assertTrue(
          schema.migrations().drift().orElse("").contains("DROP DATABASE `migration_probe_"),
          "Миграция из БД, которой нет в ветке, не попросила удалить схему"
      );
    }
  }

  @Test
  @DisplayName("Изменённый файл применённой миграции просит удалить схему")
  void changedMigrationShouldAskToDropSchema(@TempDir Path dir) {
    try (TemporarySchemaDbClient schema = new TemporarySchemaDbClient(dir)) {
      schema.write(1);
      schema.migrate();
      schema.rewrite(1);
      assertTrue(
          schema.migrations().drift().orElse("").contains("DROP DATABASE `migration_probe_"),
          "Изменённый файл применённой миграции не попросил удалить схему"
      );
    }
  }
}
