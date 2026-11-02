package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.Config;
import io.student.rangiffler.data.clients.MigrationDbClient;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.List;
import java.util.Optional;

/**
 * Перед всем прогоном сверяет схемы локальной MySQL с миграциями бэкендов текущей ветки: после переключения веток
 * без пересоздания БД код бэкендов и схема расходятся, и тесты падают на непонятных ошибках вместо одной понятной.
 */
public final class StandSchemaExtension implements SuiteExtension {

  private final List<MigrationDbClient> schemas;

  public StandSchemaExtension() {
    this(List.of(
        new MigrationDbClient(Config.authJdbcUrl(), "rangiffler-auth", Config.authMigrations()),
        new MigrationDbClient(Config.apiJdbcUrl(), "rangiffler-api", Config.apiMigrations())
    ));
  }

  public StandSchemaExtension(List<MigrationDbClient> schemas) {
    this.schemas = schemas;
  }

  @Override
  public void beforeSuite(ExtensionContext context) {
    List<String> drifts = schemas.stream().map(MigrationDbClient::drift).flatMap(Optional::stream).toList();
    if (!drifts.isEmpty()) {
      throw new IllegalStateException(
          "Схема локальной БД не совпадает с миграциями бэкендов текущей ветки, тесты не запускаются\n"
              + String.join("\n\n", drifts)
      );
    }
  }
}
