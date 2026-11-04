package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.Config;
import io.student.rangiffler.data.clients.MigrationDbClient;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import java.util.List;
import java.util.Optional;

/**
 * При открытии сессии JUnit Platform таски e2e сверяет схемы локальной MySQL с миграциями бэкендов текущей ветки: после
 * переключения веток без пересоздания БД код бэкендов и схема расходятся, и тесты падают на непонятных ошибках вместо
 * одной понятной. Сессия открывается до обнаружения тестов, поэтому сверка идёт до первого теста любого движка, но
 * по разу в каждой JVM: при maxParallelForks у каждого форка Gradle своя сессия. В остальных прогонах (test, build, IDE)
 * флаг rfr.stand.schema.check выключен, и сверка не выполняется.
 */
public final class StandSchemaExtension implements LauncherSessionListener {

  private final boolean enabled;
  private final List<MigrationDbClient> schemas;

  public StandSchemaExtension() {
    this(
        Config.standSchemaCheck(),
        List.of(
            new MigrationDbClient(Config.authJdbcUrl(), "rangiffler-auth", Config.authMigrations()),
            new MigrationDbClient(Config.apiJdbcUrl(), "rangiffler-api", Config.apiMigrations())
        )
    );
  }

  public StandSchemaExtension(boolean enabled, List<MigrationDbClient> schemas) {
    this.enabled = enabled;
    this.schemas = schemas;
  }

  @Override
  public void launcherSessionOpened(LauncherSession session) {
    System.out.println("INIT!!!!!!!!!!");
    if (enabled) {
      System.out.println("START!!!!!!!!!!");
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
