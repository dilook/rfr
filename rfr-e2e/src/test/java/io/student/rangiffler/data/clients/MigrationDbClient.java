package io.student.rangiffler.data.clients;

import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.CoreErrorCode;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.ValidateResult;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Сверяет историю Flyway одной схемы локальной MySQL с миграциями бэкенда из текущего checkout'а, ничего не меняя
 * в БД, описывает расхождение вместе с командой, которая его устраняет, и называет текущую версию схемы.
 */
public class MigrationDbClient {

  private static final Set<CoreErrorCode> PENDING = Set.of(
      CoreErrorCode.RESOLVED_VERSIONED_MIGRATION_NOT_APPLIED,
      CoreErrorCode.RESOLVED_REPEATABLE_MIGRATION_NOT_APPLIED,
      CoreErrorCode.OUTDATED_REPEATABLE_MIGRATION
  );

  private final String schema;
  private final String location;
  private final Flyway flyway;

  public MigrationDbClient(String jdbcUrl, String schema, String location) {
    this.schema = schema;
    this.location = location;
    // Без пустого ignoreMigrationPatterns Flyway по умолчанию молча пропускает миграции из БД, которых нет в ветке.
    // Без таймаутов драйвер ждёт без срока базу, которая приняла соединение и молчит, и вешает afterSuite отчёта.
    this.flyway = Flyway.configure()
        .dataSource(jdbcUrl, TestConfig.dbUsername(), TestConfig.dbPassword())
        .jdbcProperties(Map.of("connectTimeout", "5000", "socketTimeout", "5000"))
        .schemas(schema)
        .locations(location)
        .ignoreMigrationPatterns(new String[0])
        .failOnMissingLocations(true)
        .load();
  }

  @Step("Сверить схему БД с миграциями бэкенда")
  public Optional<String> drift() {
    ValidateResult result = flyway.validateWithResult();
    if (result.validationSuccessful) {
      return Optional.empty();
    }
    return Optional.of(
        "Схема `" + schema + "` расходится с миграциями " + location + ":\n"
            + result.getAllErrorMessages() + "\n"
            + (pending(result) ? restart() : recreation())
    );
  }

  public String version() {
    try {
      return Optional.ofNullable(flyway.info().current())
          .map(MigrationInfo::getVersion)
          .map(MigrationVersion::getVersion)
          .orElse("none");
    } catch (FlywayException e) {
      return "unavailable";
    }
  }

  private static boolean pending(ValidateResult result) {
    return !result.invalidMigrations.isEmpty()
        && result.invalidMigrations.stream().allMatch(output -> PENDING.contains(output.errorDetails.errorCode));
  }

  private static String restart() {
    return "Перезапустите бэкенды на текущей ветке, и Flyway применит недостающие миграции: bash ports.sh && ./gradlew runAll";
  }

  private String recreation() {
    return "Удалите схему и перезапустите бэкенды на текущей ветке, Flyway создаст её заново (данные схемы будут потеряны): "
        + "docker exec rfr-mysql mysql -u" + TestConfig.dbUsername() + " -p" + TestConfig.dbPassword()
        + " -e 'DROP DATABASE `" + schema + "`' && bash ports.sh && ./gradlew runAll";
  }
}
