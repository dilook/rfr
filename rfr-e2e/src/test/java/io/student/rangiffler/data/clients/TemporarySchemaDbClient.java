package io.student.rangiffler.data.clients;

import io.student.rangiffler.config.TestConfig;
import org.flywaydb.core.Flyway;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Временная схема локальной MySQL вместе с собственным каталогом Flyway-миграций для проб сверки схем: владеет и схемой,
 * и файлами, а при закрытии удаляет схему, даже если migrate() упал на середине.
 */
public class TemporarySchemaDbClient implements AutoCloseable {

  private final String schema;
  private final Path dir;
  private final Flyway flyway;

  public TemporarySchemaDbClient(Path dir) {
    this("migration_probe_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12), dir);
  }

  public TemporarySchemaDbClient(String schema, Path dir) {
    this.schema = schema;
    this.dir = dir;
    // URL без имени базы: clean() включён, и соединение не должно указывать на настоящую схему rangiffler-*.
    this.flyway = Flyway.configure()
        .dataSource(TestConfig.dbJdbcUrl(), TestConfig.dbUsername(), TestConfig.dbPassword())
        .schemas(schema)
        .createSchemas(true)
        .cleanDisabled(false)
        .locations("filesystem:" + dir)
        .load();
  }

  public void write(int version) {
    try {
      Files.writeString(file(version), "create table probe_" + UUID.randomUUID().toString().replace("-", "") + " (id int)");
    } catch (IOException e) {
      throw new UncheckedIOException("Не удалось записать миграцию V" + version + " временной схемы " + schema, e);
    }
  }

  public void rewrite(int version) {
    if (!Files.exists(file(version))) {
      throw new IllegalStateException("Миграции V" + version + " временной схемы " + schema + " нет, переписывать нечего");
    }
    write(version);
  }

  public void delete(int version) {
    try {
      Files.delete(file(version));
    } catch (IOException e) {
      throw new UncheckedIOException("Не удалось удалить миграцию V" + version + " временной схемы " + schema, e);
    }
  }

  public void migrate() {
    flyway.migrate();
  }

  public MigrationDbClient migrations() {
    return new MigrationDbClient(TestConfig.dbJdbcUrl(), schema, "filesystem:" + dir);
  }

  @Override
  public void close() {
    flyway.clean();
  }

  private Path file(int version) {
    return dir.resolve("V" + version + "__probe.sql");
  }
}
