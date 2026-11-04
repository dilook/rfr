package io.student.rangiffler.jupiter.probe;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.condition.ProbeTargetCondition;
import io.student.rangiffler.jupiter.extension.StandReadinessExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет StandReadinessExtension на подставном стенде: rfr-auth, rfr-api и фронт заменяет HttpServer на эфемерном
 * порту, а MySQL остаётся настоящей локальной, как в остальных пробах.
 */
class StandReadinessExtensionTest {

  @Test
  @DisplayName("Сервис начал отвечать через 2 с — тесты выполняются")
  void testsShouldRunWhenServiceAnswersLate() throws IOException {
    try (FakeService service = new FakeService(Duration.ofSeconds(2)).start()) {
      assertEquals(
          1L,
          run(service.url(), "true", "20").testEvents().succeeded().count(),
          "Тест не выполнился, хотя сервис начал отвечать до таймаута"
      );
    }
  }

  @Test
  @DisplayName("Сервис не ответил за таймаут — тесты не выполняются")
  void testsShouldNotRunWhenServiceStaysSilent() throws IOException {
    try (FakeService service = new FakeService(Duration.ofDays(1)).start()) {
      assertEquals(
          0L,
          run(service.url(), "true", "1").testEvents().started().count(),
          "Тест выполнился, хотя сервис так и не ответил"
      );
    }
  }

  @Test
  @DisplayName("Сервис не ответил за таймаут — в ошибке есть имя сервиса")
  void failureShouldNameSilentService() throws IOException {
    try (FakeService service = new FakeService(Duration.ofDays(1)).start()) {
      assertTrue(
          failures(run(service.url(), "true", "1")).anyMatch(message -> message.contains("rfr-gql-client")),
          "Ошибка не называет сервис, который не ответил"
      );
    }
  }

  @Test
  @DisplayName("Порт MySQL открыт, но SELECT 1 не выполняется — в ошибке есть MySQL")
  void failureShouldNameMysqlWhenPortAnswersWithoutDatabase() throws IOException {
    try (FakeService service = new FakeService(Duration.ZERO).start(); FakePort port = new FakePort().start()) {
      assertTrue(
          failures(run(service.url(), "true", "1", port.jdbcUrl())).anyMatch(message -> message.contains("MySQL")),
          "Ошибка не называет MySQL, хотя на её порту отвечает не база"
      );
    }
  }

  @Test
  @DisplayName("Сервис слушает только IPv6, а адрес задан как localhost — тесты выполняются")
  void testsShouldRunWhenServiceListensOnlyOnIpv6Localhost() throws IOException {
    try (FakeService service = new FakeService(Duration.ZERO, InetAddress.getByName("::1")).start()) {
      assertEquals(
          1L,
          run(service.url("localhost"), "true", "3").testEvents().succeeded().count(),
          "Тест не выполнился, хотя сервис отвечает на localhost по IPv6"
      );
    }
  }

  @Test
  @DisplayName("К сервису не приходит запрос с апгрейдом до HTTP/2")
  void serviceShouldNotReceiveHttp2Upgrade() throws IOException {
    try (FakeService service = new FakeService(Duration.ZERO).start()) {
      run(service.url(), "true", "3");
      assertEquals(0, service.upgrades(), "Проверка попросила апгрейд до HTTP/2, на котором Vite зависает");
    }
  }

  @Test
  @DisplayName("Сервис отвечает по имени хоста — запрос приходит с исходным Host")
  void serviceShouldReceiveOriginalHost() throws IOException {
    try (FakeService service = new FakeService(Duration.ZERO).start()) {
      run(service.url("localhost"), "true", "3");
      assertEquals(
          List.of(URI.create(service.url("localhost")).getAuthority()),
          service.hosts(),
          "Проверка подменила Host на IP-адрес, хотя сервис отвечал по имени хоста"
      );
    }
  }

  @Test
  @DisplayName("Проверка выключена — образец выполняется, а к сервису нет ни одного запроса")
  void disabledCheckShouldNotCallService() throws IOException {
    try (FakeService service = new FakeService(Duration.ZERO).start()) {
      assertEquals(
          List.of(1L, 0),
          List.of(run(service.url(), "false", "1").testEvents().succeeded().count(), service.hits()),
          "Выключенная проверка обратилась к сервису или образец не выполнился"
      );
    }
  }

  private static EngineExecutionResults run(String url, String check, String timeout) {
    return run(url, check, timeout, TestConfig.dbJdbcUrl());
  }

  private static EngineExecutionResults run(String url, String check, String timeout, String jdbcUrl) {
    return EngineTestKit.engine("junit-jupiter")
        .selectors(DiscoverySelectors.selectClass(StandSample.class))
        .configurationParameter(ProbeTargetCondition.ENABLED, "true")
        // Автоопределение подключило бы к образцам AllureJupiter и Stand*Extension из ServiceLoader.
        .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
        .configurationParameter("rfr.standCheck", check)
        .configurationParameter("rfr.standTimeout", timeout)
        .configurationParameter("rfr.auth.url", url)
        .configurationParameter("rfr.api.url", url)
        .configurationParameter("rfr.front.url", url)
        .configurationParameter("rfr.db.jdbc.url", jdbcUrl)
        .execute();
  }

  private static Stream<String> failures(EngineExecutionResults results) {
    return results.containerEvents().failed().stream()
        .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow().getCause())
        .filter(Objects::nonNull)
        .map(Throwable::getMessage);
  }

  /**
   * Подставной HTTP-сервис на эфемерном порту: до момента готовности отвечает 503, после — 200, и запоминает заголовки
   * каждого запроса.
   */
  private static final class FakeService implements HttpHandler, AutoCloseable {

    private final HttpServer server;
    private final Instant readiness;
    private final Queue<Headers> requests;

    FakeService(Duration delay) throws IOException {
      this(delay, InetAddress.getLoopbackAddress());
    }

    FakeService(Duration delay, InetAddress address) throws IOException {
      this(
          HttpServer.create(new InetSocketAddress(address, 0), 0),
          Instant.now().plus(delay),
          new ConcurrentLinkedQueue<>()
      );
    }

    FakeService(HttpServer server, Instant readiness, Queue<Headers> requests) {
      this.server = server;
      this.readiness = readiness;
      this.requests = requests;
    }

    FakeService start() {
      server.createContext("/", this);
      server.start();
      return this;
    }

    String url() {
      return url(server.getAddress().getHostString());
    }

    String url(String host) {
      return "http://" + host + ":" + server.getAddress().getPort();
    }

    int hits() {
      return requests.size();
    }

    int upgrades() {
      return Math.toIntExact(requests.stream().filter(headers -> headers.containsKey("Upgrade")).count());
    }

    List<String> hosts() {
      return requests.stream().map(headers -> headers.getFirst("Host")).distinct().toList();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      requests.add(exchange.getRequestHeaders());
      exchange.sendResponseHeaders(Instant.now().isBefore(readiness) ? 503 : 200, -1);
      exchange.close();
    }

    @Override
    public void close() {
      server.stop(0);
    }
  }

  /**
   * Подставной порт MySQL на эфемерном порту: принимает TCP-соединение и сразу закрывает его, не отвечая как база.
   */
  private static final class FakePort implements AutoCloseable {

    private final ServerSocket socket;

    FakePort() throws IOException {
      this(new ServerSocket(0, 50, InetAddress.getLoopbackAddress()));
    }

    FakePort(ServerSocket socket) {
      this.socket = socket;
    }

    FakePort start() {
      Thread.ofVirtual().start(() -> {
        while (!socket.isClosed()) {
          try {
            socket.accept().close();
          } catch (IOException e) {
            return;
          }
        }
      });
      return this;
    }

    String jdbcUrl() {
      return "jdbc:mysql://" + socket.getInetAddress().getHostAddress() + ":" + socket.getLocalPort() + "/?serverTimezone=UTC";
    }

    @Override
    public void close() throws IOException {
      socket.close();
    }
  }

  @ProbeTarget
  @ExtendWith(StandReadinessExtension.class)
  static class StandSample {

    @Test
    void test() {
    }
  }
}
