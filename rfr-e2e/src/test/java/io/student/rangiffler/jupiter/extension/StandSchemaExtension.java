package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.data.clients.MigrationDbClient;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.List;
import java.util.Optional;

/**
 * Перед всем прогоном таски e2e сверяет схемы локальной MySQL с миграциями бэкендов текущей ветки: после переключения
 * веток без пересоздания БД код бэкендов и схема расходятся, и тесты падают на непонятных ошибках вместо одной понятной.
 * В остальных прогонах (test, build, IDE) флаг rfr.stand.schema.check выключен, и сверка не выполняется.
 */
public final class StandSchemaExtension implements SuiteExtension {

  private final boolean enabled;
  private final List<MigrationDbClient> schemas;

  public StandSchemaExtension() {
    this(
        TestConfig.standSchemaCheck(),
        List.of(
            new MigrationDbClient(TestConfig.authJdbcUrl(), "rangiffler-auth", TestConfig.authMigrations()),
            new MigrationDbClient(TestConfig.apiJdbcUrl(), "rangiffler-api", TestConfig.apiMigrations())
        )
    );
  }

  public StandSchemaExtension(boolean enabled, List<MigrationDbClient> schemas) {
    this.enabled = enabled;
    this.schemas = schemas;
  }

  @Override
  public void beforeSuite(ExtensionContext context) {
    if (enabled) {
      List<String> drifts = schemas.stream().map(MigrationDbClient::drift).flatMap(Optional::stream).toList();
      if (!drifts.isEmpty()) {
        throw new IllegalStateException(
            "Схема локальной БД не совпадает с миграциями бэкендов текущей ветки, тесты не запускаются\n"
                + String.join("\n\n", drifts)
        );
      }
    }
  }
}
