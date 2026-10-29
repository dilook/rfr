package io.student.rangiffler.data.clients;

import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class ApiUserDbClient {

  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transactionTemplate;

  public ApiUserDbClient() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource(
        Config.apiJdbcUrl(), Config.dbUsername(), Config.dbPassword()
    );
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    this.transactionTemplate = new TransactionTemplate(new JdbcTransactionManager(dataSource));
  }

  @Step("Создать пользователя в БД rangiffler-api: {username}")
  public void createUser(String username) {
    jdbcTemplate.update(
        "insert into `user` (username, country_id) values (?, (select id from `country` where code = 'ru'))",
        username
    );
  }

  @Step("Удалить пользователя из БД rangiffler-api: {username}")
  public void deleteUserByUsername(String username) {
    transactionTemplate.executeWithoutResult(status -> {
      for (byte[] userId : jdbcTemplate.queryForList("select id from `user` where username = ?", byte[].class, username)) {
        jdbcTemplate.update(
            "delete from `photo_like` "
                + "where photo_id in (select id from `photo` where user_id = ?) "
                + "or like_id in (select id from `like` where user_id = ?)",
            userId, userId
        );
        jdbcTemplate.update("delete from `like` where user_id = ?", userId);
        jdbcTemplate.update("delete from `photo` where user_id = ?", userId);
        jdbcTemplate.update("delete from `statistic` where user_id = ?", userId);
        jdbcTemplate.update("delete from `friendship` where requester_id = ? or addressee_id = ?", userId, userId);
        jdbcTemplate.update("delete from `user` where id = ?", userId);
      }
    });
  }
}
