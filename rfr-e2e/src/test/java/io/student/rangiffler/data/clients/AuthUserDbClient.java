package io.student.rangiffler.data.clients;

import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

public class AuthUserDbClient {

    private static final PasswordEncoder PASSWORD_ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public AuthUserDbClient() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                Config.authJdbcUrl(), Config.dbUsername(), Config.dbPassword()
        );
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(new JdbcTransactionManager(dataSource));
    }

    @Step("Создать пользователя в БД rangiffler-auth: {username} / {password}")
    public void createUser(String username, String password) {
        UUID id = UUID.randomUUID();
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update(
                    "insert into `user` (id, username, password, enabled, account_non_expired, account_non_locked, credentials_non_expired) "
                            + "values (UUID_TO_BIN(?, true), ?, ?, true, true, true, true)",
                    id.toString(), username, PASSWORD_ENCODER.encode(password)
            );
            for (String authority : new String[]{"read", "write"}) {
                jdbcTemplate.update(
                        "insert into `authority` (user_id, authority) values (UUID_TO_BIN(?, true), ?)",
                        id.toString(), authority
                );
            }
        });
    }

    @Step("Проверить, есть ли пользователь в БД rangiffler-auth: {username}")
    public boolean userExists(String username) {
        return jdbcTemplate.queryForObject("select count(*) from `user` where username = ?", Integer.class, username) > 0;
    }

    @Step("Удалить пользователя из БД rangiffler-auth: {username}")
    public void deleteUserByUsername(String username) {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update(
                    "delete from `authority` where user_id in (select id from `user` where username = ?)", username
            );
            jdbcTemplate.update("delete from `user` where username = ?", username);
        });
    }
}
