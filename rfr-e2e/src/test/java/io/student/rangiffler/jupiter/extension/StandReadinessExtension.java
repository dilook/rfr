package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.data.clients.HttpStandService;
import io.student.rangiffler.data.clients.JdbcStandService;
import io.student.rangiffler.data.clients.StandService;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Перед прогоном ждёт, пока MySQL не выполнит SELECT 1, а rfr-auth, rfr-api и фронт не ответят по тем же признакам,
 * что scripts/wait-for-stand.sh: прогон, начатый вместе с подъёмом стенда, не падает на первом запросе, а при недоступном
 * стенде падает одной понятной ошибкой вместо таймаута Playwright. Настройки читаются из параметров конфигурации JUnit
 * и только без них из TestConfig, чтобы пробы подставляли свой стенд, не трогая system properties соседних тестов.
 */
public final class StandReadinessExtension implements SuiteExtension {

  private final Duration pause;
  private final Duration probe;

  public StandReadinessExtension() {
    this(Duration.ofSeconds(2), Duration.ofSeconds(5));
  }

  public StandReadinessExtension(Duration pause, Duration probe) {
    this.pause = pause;
    this.probe = probe;
  }

  @Override
  public void beforeSuite(ExtensionContext context) {
    if (context.getConfigurationParameter("rfr.standCheck", Boolean::parseBoolean).orElseGet(TestConfig::standCheck)) {
      await(
          services(context),
          context.getConfigurationParameter("rfr.standTimeout", Long::parseLong)
              .map(Duration::ofSeconds)
              .orElseGet(TestConfig::standTimeout)
      );
    }
  }

  private void await(List<StandService> services, Duration timeout) {
    Instant deadline = Instant.now().plus(timeout);
    List<StandService> silent = services.stream().filter(service -> !service.ready(deadline)).toList();
    while (!silent.isEmpty()) {
      Duration remaining = Duration.between(Instant.now(), deadline);
      if (remaining.isNegative() || remaining.isZero()) {
        throw new IllegalStateException(
            "Стенд не ответил за " + timeout.toSeconds() + " с, тесты не запускаются\n"
                + silent.stream().map(StandService::outage).collect(Collectors.joining("\n"))
        );
      }
      sleep(remaining.compareTo(pause) < 0 ? remaining : pause);
      silent = silent.stream().filter(service -> !service.ready(deadline)).toList();
    }
  }

  private void sleep(Duration duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Ожидание стенда прервано", e);
    }
  }

  private List<StandService> services(ExtensionContext context) {
    return List.of(
        new JdbcStandService(
            "MySQL", setting(context, "rfr.db.jdbc.url", TestConfig::dbJdbcUrl), probe, "docker compose up -d --wait"
        ),
        new HttpStandService(
            "rfr-auth",
            HttpRequest.newBuilder(
                URI.create(setting(context, "rfr.auth.url", TestConfig::authUrl) + "/.well-known/openid-configuration")
            )
                .timeout(probe)
                .GET()
                .build(),
            status -> status < 400,
            "./gradlew runAll"
        ),
        // Как curl без -f в wait-for-stand.sh: любой HTTP-ответ, даже 4xx/5xx, значит, что rfr-api принимает запросы.
        new HttpStandService(
            "rfr-api",
            HttpRequest.newBuilder(URI.create(setting(context, "rfr.api.url", TestConfig::apiUrl) + "/graphql"))
                .timeout(probe)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"{__typename}\"}"))
                .build(),
            status -> true,
            "./gradlew runAll"
        ),
        new HttpStandService(
            "rfr-gql-client",
            HttpRequest.newBuilder(URI.create(setting(context, "rfr.front.url", TestConfig::frontUrl)))
                .timeout(probe)
                .GET()
                .build(),
            status -> status < 400,
            "cd rfr-gql-client && npm run dev"
        )
    );
  }

  private String setting(ExtensionContext context, String key, Supplier<String> fallback) {
    return context.getConfigurationParameter(key).orElseGet(fallback);
  }
}
