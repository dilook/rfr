package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.config.Config;
import io.student.rangiffler.data.clients.MigrationDbClient;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет сверку схемы с миграциями бэкенда на временной схеме локальной MySQL, которую сам тест накатывает
 * Flyway'ем из временного каталога и удаляет в конце.
 */
class MigrationDbClientTest {

  @Test
  @DisplayName("Схема, совпадающая с миграциями, не даёт расхождения")
  void matchingSchemaShouldHaveNoDrift(@TempDir Path dir) throws IOException {
    String schema = schema();
    Files.writeString(dir.resolve("V1__init.sql"), "create table kettle_7f (id int)");
    Flyway flyway = migrated(schema, dir);
    try {
      assertEquals(Optional.empty(), client(schema, dir).drift(), "Совпадающая схема дала расхождение");
    } finally {
      flyway.clean();
    }
  }

  @Test
  @DisplayName("Неприменённая миграция просит перезапустить бэкенды, не удаляя схему")
  void pendingMigrationShouldAskToRestartBackends(@TempDir Path dir) throws IOException {
    String schema = schema();
    Files.writeString(dir.resolve("V1__init.sql"), "create table walrus_3c (id int)");
    Flyway flyway = migrated(schema, dir);
    try {
      Files.writeString(dir.resolve("V2__more.sql"), "create table quince_9a (id int)");
      assertTrue(
          client(schema, dir).drift().orElse("").contains("Flyway применит недостающие миграции"),
          "Неприменённая миграция не попросила перезапустить бэкенды"
      );
    } finally {
      flyway.clean();
    }
  }

  @Test
  @DisplayName("Миграция из БД, которой нет в ветке, просит удалить схему")
  void futureMigrationShouldAskToDropSchema(@TempDir Path dir) throws IOException {
    String schema = schema();
    Files.writeString(dir.resolve("V1__init.sql"), "create table lantern_5e (id int)");
    Files.writeString(dir.resolve("V2__more.sql"), "create table pebble_1d (id int)");
    Flyway flyway = migrated(schema, dir);
    try {
      Files.delete(dir.resolve("V2__more.sql"));
      assertTrue(
          client(schema, dir).drift().orElse("").contains("DROP DATABASE `" + schema + "`"),
          "Миграция из БД, которой нет в ветке, не попросила удалить схему"
      );
    } finally {
      flyway.clean();
    }
  }

  @Test
  @DisplayName("Изменённый файл применённой миграции просит удалить схему")
  void changedMigrationShouldAskToDropSchema(@TempDir Path dir) throws IOException {
    String schema = schema();
    Files.writeString(dir.resolve("V1__init.sql"), "create table harbor_8b (id int)");
    Flyway flyway = migrated(schema, dir);
    try {
      Files.writeString(dir.resolve("V1__init.sql"), "create table harbor_8b (id bigint)");
      assertTrue(
          client(schema, dir).drift().orElse("").contains("DROP DATABASE `" + schema + "`"),
          "Изменённый файл применённой миграции не попросил удалить схему"
      );
    } finally {
      flyway.clean();
    }
  }

  private static String schema() {
    return "migration_probe_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }

  private static MigrationDbClient client(String schema, Path dir) {
    return new MigrationDbClient(Config.apiJdbcUrl(), schema, "filesystem:" + dir);
  }

  private static Flyway migrated(String schema, Path dir) {
    Flyway flyway = Flyway.configure()
        .dataSource(Config.apiJdbcUrl(), Config.dbUsername(), Config.dbPassword())
        .schemas(schema)
        .createSchemas(true)
        .cleanDisabled(false)
        .locations("filesystem:" + dir)
        .load();
    flyway.migrate();
    return flyway;
  }
}
