## Назначение

Spring Authorization Server, Thymeleaf-страницы login/register. Эндпоинты, процессы входа, регистрации и logout — `rfr-auth/README.md`.

## Запуск

```bash
./gradlew :rfr-auth:bootRun
curl http://localhost:9001/.well-known/openid-configuration
```

## Клиент и токены

- RSA-ключ генерируется при каждом старте — после рестарта все токены невалидны.
- `/oauth2/revoke` для публичного клиента из коробки отвечает `401 invalid_client`; работает только благодаря `PublicClientRevocationAuthenticationConverter/Provider`.
- Два `SecurityFilterChain` в `RangifflerAuthServiceConfig`.
- OAuth-авторизации и HTTP-сессии хранятся в памяти и теряются при перезапуске.
