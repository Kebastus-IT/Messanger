# Messanger

Kotlin Multiplatform messenger with a Compose client and a Ktor server.

The repository contains:

- `composeApp` - Compose Multiplatform client for Android, Desktop JVM, Web JS and WebAssembly
- `server` - Ktor backend with JWT auth, REST endpoints and WebSocket chat delivery
- `shared` - shared DTOs and WebSocket protocol used by both client and server

## What is implemented

- registration and login
- JWT-based authentication
- session persistence on the client via `multiplatform-settings`
- chat list loading for the authenticated user
- user search by login
- direct message creation or reuse
- WebSocket connection to a chat
- loading recent messages after join
- sending messages to all online chat members
- PostgreSQL persistence with Flyway migrations and Exposed

Not implemented yet:

- group chat creation in UI and backend flow
- production-ready configuration via environment variables
- meaningful automated tests

## Tech stack

- Kotlin Multiplatform
- Compose Multiplatform
- Ktor Client and Ktor Server
- Kotlinx Serialization
- PostgreSQL
- Flyway
- Exposed
- HikariCP
- JWT
- Argon2

## Project structure

```text
.
|- composeApp/
|  |- src/commonMain/...      # shared UI, screens, HTTP and WS client code
|  |- src/androidMain/...     # Android entrypoint and Android base URL
|  |- src/jvmMain/...         # Desktop entrypoint and localhost base URL
|  |- src/webMain/...         # Web entrypoint and localhost base URL
|- server/
|  |- src/main/kotlin/...     # Ktor app, auth, repositories, websocket handlers
|  |- src/main/resources/db/  # Flyway migrations
|- shared/
|  |- src/commonMain/...      # auth models, chat models, WS protocol
```

## Backend API

### REST

- `POST /register`
- `POST /login`
- `GET /users/find?login=...` - requires `Authorization: Bearer <token>`
- `POST /chats/dm` - requires `Authorization: Bearer <token>`
- `GET /chats` - requires `Authorization: Bearer <token>`

### WebSocket

- `GET /ws` - requires `Authorization: Bearer <token>`
- `GET /echo` - simple echo test socket without auth

### WebSocket protocol

Client events:

- `join`
- `message`

Server events:

- `connected`
- `joined_chat`
- `recent_messages`
- `chat_message`
- `error`

The JSON discriminator is `#type`.

## Database

The server connects to PostgreSQL with the following hardcoded settings:

- database: `messenger`
- user: `messenger`
- password: `messenger`
- host: `localhost`
- port: `5432`

These values currently live in `server/src/main/kotlin/org/messanger/project/database/Db.kt`.

Flyway migrations create:

- `users`
- `chats`
- `chat_members`
- `messages`

The initial migration also inserts demo records:

- users `leonid` and `alex`
- group chat `room-general`
- legacy demo DM `dm-u1-u2`

Current direct messages created by the app use IDs in the form `dm:<userA>:<userB>`.

## Local run

### 1. Start PostgreSQL

```powershell
docker compose up -d
```

### 2. Start the server

```powershell
.\gradlew.bat :server:run
```

The server listens on `http://localhost:8080`.

### 3. Start a client

Desktop:

```powershell
.\gradlew.bat :composeApp:run
```

Web:

```powershell
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
```

Android debug build:

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Base URLs are platform-specific:

- Android emulator: `http://10.0.2.2:8080`
- Desktop/Web: `http://localhost:8080`

## Manual testing

There is a simple HTTP client file in `test-auth.http` with register and login requests.

You can also use the demo accounts inserted by the initial migration only after replacing their placeholder password hashes. As checked in now, those seeded hashes are not valid Argon2 hashes for real login.

## Known issues

- `server/src/test/kotlin/.../ApplicationTest.kt` is still the default template test and does not match the current server routes.
- JWT secret and database credentials are committed in source code and config, which is acceptable only for local development.
- `Db.kt` does not read environment variables yet, so deployments require code changes.
- some source files still contain draft or debug-level code/comments and need cleanup before production use.

## Useful commands

```powershell
.\gradlew.bat :server:run
.\gradlew.bat :composeApp:run
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
.\gradlew.bat :composeApp:assembleDebug
```
