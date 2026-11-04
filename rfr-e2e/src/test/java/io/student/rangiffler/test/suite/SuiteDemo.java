package io.student.rangiffler.test.suite;


import io.student.rangiffler.config.Config;
import io.student.rangiffler.data.clients.MigrationDbClient;
import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Suite
@SelectPackages("io.student.rangiffler.test")
public class SuiteDemo {


  @BeforeSuite
  public static void beforeSuite() {
    System.out.println("START!!");

    if (Config.standSchemaCheck()) {
      List<String> drifts = Stream.of(
          new MigrationDbClient(Config.authJdbcUrl(), "rangiffler-auth", Config.authMigrations()),
          new MigrationDbClient(Config.apiJdbcUrl(), "rangiffler-api", Config.apiMigrations())
      ).map(MigrationDbClient::drift).flatMap(Optional::stream).toList();
      if (!drifts.isEmpty()) {
        throw new IllegalStateException(
            "Схема локальной БД не совпадает с миграциями бэкендов текущей ветки, тесты не запускаются\n"
                + String.join("\n\n", drifts)
        );
      }
    }
  }

  @AfterSuite
  public static void afterSuite() {
    System.out.println("END!!");

  }
}
