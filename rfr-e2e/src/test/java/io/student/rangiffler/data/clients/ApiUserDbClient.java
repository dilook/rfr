package io.student.rangiffler.data.clients;

import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

public class ApiUserDbClient {

  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transactionTemplate;

  public ApiUserDbClient() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource(
        TestConfig.apiJdbcUrl(), TestConfig.dbUsername(), TestConfig.dbPassword()
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

  @Step("Сделать друзьями в БД rangiffler-api: {requester} и {addressee}")
  public void createFriendship(String requester, String addressee) {
    jdbcTemplate.update(
        "insert into `friendship` (id, requester_id, addressee_id, status, created_at, responded_at) "
            + "values (UUID_TO_BIN(UUID(), true), (select id from `user` where username = ?), "
            + "(select id from `user` where username = ?), 'ACCEPTED', now(6), now(6))",
        requester, addressee
    );
  }

  @Step("Отправить приглашение в друзья в БД rangiffler-api: {requester} → {addressee}")
  public void createInvitation(String requester, String addressee) {
    jdbcTemplate.update(
        "insert into `friendship` (id, requester_id, addressee_id, status, created_at) "
            + "values (UUID_TO_BIN(UUID(), true), (select id from `user` where username = ?), "
            + "(select id from `user` where username = ?), 'PENDING', now(6))",
        requester, addressee
    );
  }

  @Step("Найти дружбу в БД rangiffler-api: {requester} → {addressee} со статусом {status}")
  public List<String> friendshipIds(String requester, String addressee, String status) {
    return jdbcTemplate.queryForList(
        "select BIN_TO_UUID(id, true) from `friendship` "
            + "where requester_id = (select id from `user` where username = ?) "
            + "and addressee_id = (select id from `user` where username = ?) and status = ?",
        String.class, requester, addressee, status
    );
  }

  @Step("Проверить, есть ли дружба в БД rangiffler-api: {id}")
  public boolean friendshipExists(String id) {
    return jdbcTemplate.queryForObject("select count(*) from `friendship` where id = UUID_TO_BIN(?, true)", Integer.class, id) > 0;
  }

  @Step("Проверить, есть ли пользователь в БД rangiffler-api: {username}")
  public boolean userExists(String username) {
    return jdbcTemplate.queryForObject("select count(*) from `user` where username = ?", Integer.class, username) > 0;
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
