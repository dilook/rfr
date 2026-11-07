## Назначение

Входит через `rfr-auth` по Authorization Code + PKCE. Маршруты, localStorage и список операций — `rfr-gql-client/README.md`.

## Запуск

Нужны запущенные `rfr-auth` (9001) и `rfr-api` (8081).

```bash
cd rfr-gql-client
npm ci
npm run dev     # http://localhost:3001
npm run build   # tsc + vite build
npm run lint    # eslint, --max-warnings 0
```

## Соглашения

- Таблицы People (`PeopleTable`) до прихода данных показывают `LinearProgress` «Loading people», а пустое состояние — только после ответа, в том числе с ошибкой: на это опираются e2e-проверки пустых вкладок, которые после сообщения проверяют отсутствие `alert`.
- Текст ошибки показывается пользователю только для `BAD_REQUEST`/`NOT_FOUND` (`src/api/graphqlError.ts`), остальное — общим сообщением.
- Порт dev-сервера 3001 должен совпадать с redirect URI клиента в `rfr-auth`.
- Компоненты берутся из MUI, свои с нуля не пишутся.