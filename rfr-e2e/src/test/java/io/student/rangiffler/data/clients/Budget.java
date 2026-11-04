package io.student.rangiffler.data.clients;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Время на одну попытку опроса сервиса стенда: таймаут попытки, но не дольше остатка до общего срока ожидания. Результат
 * усечён до миллисекунд и не бывает отрицательным, потому что драйверы читают ноль как «ждать вечно», а HttpClient его
 * отвергает; попытку с нулевым бюджетом вызывающий не делает вовсе.
 */
final class Budget {

  private final Duration limit;
  private final Instant deadline;

  Budget(Duration limit, Instant deadline) {
    this.limit = limit;
    this.deadline = deadline;
  }

  Duration duration() {
    Duration remaining = Duration.between(Instant.now(), deadline);
    if (remaining.isNegative()) {
      return Duration.ZERO;
    }
    return (remaining.compareTo(limit) < 0 ? remaining : limit).truncatedTo(ChronoUnit.MILLIS);
  }
}
