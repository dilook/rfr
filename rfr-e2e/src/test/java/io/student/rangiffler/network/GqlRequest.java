package io.student.rangiffler.network;

import com.microsoft.playwright.Request;
import io.student.rangiffler.config.TestConfig;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * GraphQL-запрос страницы поверх Request Playwright: operationName и variables из JSON-тела. waitForResponse показывает
 * предикату все ответы страницы, поэтому isOperation() отвечает false для запроса не на /graphql API, а operationName()
 * и variables() на таком запросе падают.
 */
public final class GqlRequest {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private final Request request;

  public GqlRequest(Request request) {
    this.request = request;
  }

  public boolean isOperation(String name) {
    return isGql() && operationName().equals(name);
  }

  public String operationName() {
    JsonNode name = body().path("operationName");
    if (!name.isString()) {
      throw new IllegalStateException("Запрос " + request.method() + " " + request.url() + " не содержит operationName GraphQL");
    }
    return name.stringValue();
  }

  public JsonNode variables() {
    JsonNode variables = body().path("variables");
    if (!variables.isObject()) {
      throw new IllegalStateException("Запрос " + request.method() + " " + request.url() + " не содержит variables GraphQL");
    }
    return variables;
  }

  private boolean isGql() {
    return request.url().equals(TestConfig.apiUrl() + "/graphql");
  }

  private JsonNode body() {
    if (!isGql()) {
      throw new IllegalStateException("Запрос " + request.method() + " " + request.url() + " не является GraphQL-запросом к " + TestConfig.apiUrl() + "/graphql");
    }
    String data = request.postData();
    if (data == null) {
      throw new IllegalStateException("GraphQL-запрос " + request.method() + " " + request.url() + " пришёл без тела");
    }
    try {
      return MAPPER.readTree(data);
    } catch (JacksonException e) {
      throw new IllegalStateException("Тело GraphQL-запроса " + request.method() + " " + request.url() + " не является JSON", e);
    }
  }
}
