package io.student.rangiffler.stand;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Стенд Rangiffler, готовый, когда MySQL выполняет SELECT 1, а rfr-auth, rfr-api и фронт отвечают по тем же признакам,
 * что scripts/wait-for-stand.sh.
 */
public final class Stand {

  private final List<StandService> services;
  private final Duration timeout;
  private final Duration pause;

  public Stand(String database, String auth, String api, String front, Duration timeout) {
    this(database, auth, api, front, timeout, Duration.ofSeconds(5));
  }

  private Stand(String database, String auth, String api, String front, Duration timeout, Duration probe) {
    this(
        List.of(
            new JdbcStandService("MySQL", database, probe, "docker compose up -d --wait"),
            new HttpStandService(
                "rfr-auth",
                HttpRequest.newBuilder(URI.create(auth + "/.well-known/openid-configuration"))
                    .timeout(probe)
                    .GET()
                    .build(),
                status -> status < 400,
                "./gradlew runAll"
            ),
            // Как curl без -f в wait-for-stand.sh: любой HTTP-ответ, даже 4xx/5xx, значит, что rfr-api принимает запросы.
            new HttpStandService(
                "rfr-api",
                HttpRequest.newBuilder(URI.create(api + "/graphql"))
                    .timeout(probe)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"{__typename}\"}"))
                    .build(),
                status -> true,
                "./gradlew runAll"
            ),
            new HttpStandService(
                "rfr-gql-client",
                HttpRequest.newBuilder(URI.create(front)).timeout(probe).GET().build(),
                status -> status < 400,
                "cd rfr-gql-client && npm run dev"
            )
        ),
        timeout,
        Duration.ofSeconds(2)
    );
  }

  public Stand(List<StandService> services, Duration timeout, Duration pause) {
    this.services = services;
    this.timeout = timeout;
    this.pause = pause;
  }

  public void await() {
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
}
