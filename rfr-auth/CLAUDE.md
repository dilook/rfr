## Назначение

Spring Authorization Server, Thymeleaf-страницы login/register. Эндпоинты, процессы входа, регистрации и logout — `rfr-auth/README.md`.

## Запуск

```bash
./gradlew :rfr-auth:bootRun
curl http://localhost:9001/.well-known/openid-configuration
```

## Клиент и токены

- Публичный клиент `client`, Authorization Code + PKCE, refresh token не выдаётся.
- `AccessTokenCustomizer` кладёт в **access token** `aud: rangiffler-api` и claim `authorities` (`read`/`write` из таблицы `authority`).
- RSA-ключ генерируется при каждом старте — после рестарта все токены невалидны.
- `/oauth2/revoke` для публичного клиента работает благодаря `PublicClientRevocationAuthenticationConverter/Provider`.
- Два `SecurityFilterChain` в `RangifflerAuthServiceConfig`.
- OAuth-авторизации и сессии хранятся в памяти и теряются при перезапуске.

## Подводные камни

- TTL access token в документации указан по-разному (10 и 30 минут); фактическое значение —
`access-token-time-to-live` в `rfr-auth/src/main/resources/application.yaml` (сейчас `30m`).
