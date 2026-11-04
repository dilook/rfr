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
 * запрос идёт по HTTP/1.1, потому что Vite зависает на апгрейде до HTTP/2, который Java просит по умолчанию. Сначала
 * запрос уходит по имени хоста, с исходными Host и TLS; только если он не прошёл, http-запрос повторяется на каждый
 * адрес хоста: Vite слушает localhost только по IPv6, а Java по умолчанию ходит на localhost по IPv4. Для https адрес
 * не подменяется, иначе сертификат не сойдётся с именем.
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
      return answers(client, request)
          || "http".equalsIgnoreCase(request.uri().getScheme())
          && Arrays.stream(InetAddress.getAllByName(request.uri().getHost()))
          .anyMatch(address -> answers(client, direct(address)));
    } catch (UnknownHostException e) {
      return false;
    }
  }

  @Override
  public String outage() {
    return name + " (" + request.method() + " " + request.uri() + ") не ответил, поднять: " + command;
  }

  private boolean answers(HttpClient client, HttpRequest attempt) {
    try {
      return accepted.test(client.send(attempt, HttpResponse.BodyHandlers.discarding()).statusCode());
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Опрос сервиса " + name + " прерван", e);
    }
  }

  private HttpRequest direct(InetAddress address) {
    URI uri = request.uri();
    try {
      return HttpRequest.newBuilder(request, (header, value) -> true)
          .uri(
              new URI(
                  uri.getScheme(), uri.getUserInfo(), address.getHostAddress(), uri.getPort(), uri.getPath(),
                  uri.getQuery(), uri.getFragment()
              )
          )
          .build();
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Адрес " + address + " сервиса " + name + " не собирается в URI", e);
    }
  }
}
