## Назначение

SPA на React 19 + Vite 8 + Apollo Client 4 + MUI 9; данные берёт из `rfr-api` по GraphQL, входит через `rfr-auth`
по Authorization Code + PKCE. Маршруты, localStorage и список операций — `rfr-gql-client/README.md`.

## Запуск

Собирается отдельно от Gradle. Нужны запущенные `rfr-auth` (9001) и `rfr-api` (8081).

```bash
cd rfr-gql-client
npm ci
npm run dev     # http://localhost:3001
npm run build   # tsc + vite build
npm run lint    # eslint, --max-warnings 0
```

## Соглашения

- Каждая GraphQL-операция — отдельный хук в `src/hooks` с именованной операцией (`query GetFeed`, `mutation FriendshipAction`).
  E2E-тесты перехватывают запросы по `operationName`, поэтому переименование операции требует правки тестов.
- Порт dev-сервера 3001 должен совпадать с redirect URI клиента в `rfr-auth`.
- Кэш Apollo сразу обновляет карточку ответом мутации. Успешный snackbar не доказывает сохранение: проверяй повторным чтением.
- Компоненты берутся из MUI, свои с нуля не пишутся.