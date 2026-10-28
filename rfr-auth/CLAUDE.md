## Назначение

Spring Authorization Server, схема `rangiffler-auth`, Thymeleaf-страницы login/register. Эндпоинты, процессы входа, регистрации и logout — `rfr-auth/README.md`.

## Запуск

```bash
./gradlew :rfr-auth:bootRun
curl http://localhost:9001/.well-known/openid-configuration
```

## Клиент и токены

- `/oauth2/revoke` для публичного клиента из коробки отвечает `401 invalid_client`; работает только благодаря `PublicClientRevocationAuthenticationConverter/Provider`.
- Два `SecurityFilterChain` в `RangifflerAuthServiceConfig`.
- OAuth-авторизации и HTTP-сессии хранятся в памяти и теряются при перезапуске.
- Экрана согласия нет, хотя `require-authorization-consent: true`: фронт запрашивает только `scope=openid`, а для него согласие не спрашивается.
- `GET /login` без сохранённого OAuth-запроса уводит на фронтенд — вход начинать с `/oauth2/authorize` или кнопки Login на фронте.
