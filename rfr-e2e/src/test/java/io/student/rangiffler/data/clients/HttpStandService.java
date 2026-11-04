package io.student.rangiffler.data.clients;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.function.IntPredicate;

/**
 * Сервис стенда, готовый, когда на запрос пришёл ответ с подходящим статусом. Соединение и ответ ограничены таймаутом
 * самого запроса, поэтому запрос без таймаута отвергается, а не ждёт бесконечно. Как curl в scripts/wait-for-stand.sh,
 * запрос идёт по HTTP/1.1 на каждый адрес хоста по очереди: Vite слушает localhost только по IPv6 и зависает на
 * апгрейде до HTTP/2, а Java по умолчанию ходит на localhost по IPv4 и просит такой апгрейд.
 */
public final class HttpStandService implements StandService {

  private final String name;
  private final HttpRequest request;
  private final IntPredicate accepted;
  private final String command;

  public HttpStandService(String name, HttpRequest request, IntPredicate accepted, String command) {
    this.name = name;
    this.request = request;
    this.accepted = accepted;
    this.command = command;
  }

  @Override
  public boolean ready() {
    try (
        HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(
                request.timeout().orElseThrow(
                    () -> new IllegalStateException("Запрос к сервису " + name + " без таймаута ждал бы бесконечно")
                )
            )
            .build()
    ) {
      return Arrays.stream(InetAddress.getAllByName(request.uri().getHost())).anyMatch(address -> answers(client, address));
    } catch (UnknownHostException e) {
      return false;
    }
  }

  @Override
  public String outage() {
    return name + " (" + request.method() + " " + request.uri() + ") не ответил, поднять: " + command;
  }

  private boolean answers(HttpClient client, InetAddress address) {
    try {
      return accepted.test(
          client.send(
              HttpRequest.newBuilder(request, (header, value) -> true).uri(direct(address)).build(),
              HttpResponse.BodyHandlers.discarding()
          ).statusCode()
      );
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Опрос сервиса " + name + " прерван", e);
    }
  }

  private URI direct(InetAddress address) {
    URI uri = request.uri();
    try {
      return new URI(
          uri.getScheme(), uri.getUserInfo(), address.getHostAddress(), uri.getPort(), uri.getPath(), uri.getQuery(),
          uri.getFragment()
      );
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Адрес " + address + " сервиса " + name + " не собирается в URI", e);
    }
  }
}
