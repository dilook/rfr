package io.student.rangiffler.data.clients;

import io.student.rangiffler.config.TestConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

/**
 * База стенда, готовая, когда на её JDBC-подключении выполняется SELECT 1: открытый порт ещё не значит, что MySQL
 * принимает логин и запросы. Подключение и каждое чтение ответа ограничены таймаутами драйвера, но не дольше остатка
 * до срока ожидания стенда; драйвер считает таймаут на каждое чтение, а не на попытку, поэтому база, отвечающая по
 * байту, может растянуть попытку за срок. Отказ в доступе не ждёт срока, потому что неверные учётные данные со временем
 * не исправятся.
 */
public final class JdbcStandService implements StandService {

  private final String name;
  private final String url;
  private final Duration timeout;
  private final String command;

  public JdbcStandService(String name, String url, Duration timeout, String command) {
    this.name = name;
    this.url = url;
    this.timeout = timeout;
    this.command = command;
  }

  @Override
  public boolean ready(Instant deadline) {
    Duration budget = new Budget(timeout, deadline).duration();
    if (budget.isZero()) {
      return false;
    }
    Properties properties = new Properties();
    properties.setProperty("user", TestConfig.dbUsername());
    properties.setProperty("password", TestConfig.dbPassword());
    properties.setProperty("connectTimeout", String.valueOf(budget.toMillis()));
    properties.setProperty("socketTimeout", String.valueOf(budget.toMillis()));
    try (
        Connection connection = DriverManager.getConnection(url, properties);
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery("SELECT 1")
    ) {
      return result.next() && result.getInt(1) == 1;
    } catch (SQLException e) {
      if (String.valueOf(e.getSQLState()).startsWith("28")) {
        throw new IllegalStateException(name + " (" + url + ") отверг логин " + TestConfig.dbUsername(), e);
      }
      return false;
    }
  }

  @Override
  public String outage() {
    return name + " (" + url + ") не выполнил SELECT 1, поднять: " + command;
  }
}
