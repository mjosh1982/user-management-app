# User Management Service

A small Spring Boot 3 REST API for managing users. Users are kept in memory (a `ConcurrentHashMap`), behind a repository interface so the storage can later be replaced with MySQL via Spring Data JPA without touching the controller or service layers.

---

## Table of Contents

1. [Overview](#overview)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Architecture](#architecture)
5. [Build and Run](#build-and-run)
6. [API Reference](#api-reference)
7. [Error Handling](#error-handling)
8. [API Documentation (Swagger / OpenAPI)](#api-documentation-swagger--openapi)
9. [Testing](#testing)
10. [Extending to MySQL](#extending-to-mysql)

---

## Overview

The service exposes CRUD operations on a `User` resource under `/api/users`:

| Field       | Type                              | Notes                          |
|-------------|-----------------------------------|--------------------------------|
| `id`        | `Long`                            | Assigned by the server         |
| `firstName` | `String`                          | Required, max 50 characters    |
| `lastName`  | `String`                          | Required, max 50 characters    |
| `age`       | `int`                             | 0 – 150                        |
| `gender`    | `Gender` enum (`MALE`, `FEMALE`, `OTHER`) | Required               |

On startup the store is seeded with three sample users:

| id | firstName | lastName | age | gender |
|----|-----------|----------|-----|--------|
| 1  | Alice     | Smith    | 30  | FEMALE |
| 2  | Bob       | Johnson  | 42  | MALE   |
| 3  | Charlie   | Brown    | 25  | OTHER  |

Data lives only in memory, so it resets every time the application restarts.

## Tech Stack

| Component        | Version / Library                                         |
|------------------|-----------------------------------------------------------|
| Language         | Java 21                                                   |
| Framework        | Spring Boot 3.5.x (`spring-boot-starter-web`)             |
| Validation       | Jakarta Bean Validation (`spring-boot-starter-validation`)|
| Boilerplate      | Lombok                                                    |
| API docs         | springdoc-openapi 2.8.x (`springdoc-openapi-starter-webmvc-ui`) |
| Build            | Maven 3.6.3+                                              |
| Testing          | JUnit 5, Spring Boot Test, MockMvc, AssertJ               |

## Project Structure

```
user-management-service/
├── pom.xml
├── README.md
├── README.html
└── src
    ├── main
    │   ├── java/com/example/usermanagement
    │   │   ├── UserManagementApplication.java     # Spring Boot entry point + OpenAPI info
    │   │   ├── controller/UserController.java     # REST endpoints + Swagger annotations
    │   │   ├── dto/UserRequest.java               # Validated request body for POST/PUT
    │   │   ├── exception/
    │   │   │   ├── ApiException.java              # Base class for application errors
    │   │   │   ├── ErrorCode.java                 # Machine-readable error codes → HTTP status
    │   │   │   ├── ErrorResponse.java             # JSON error body
    │   │   │   ├── GlobalExceptionHandler.java    # @RestControllerAdvice
    │   │   │   ├── UserAlreadyExistsException.java
    │   │   │   └── UserNotFoundException.java
    │   │   ├── model/
    │   │   │   ├── Gender.java                    # MALE / FEMALE / OTHER
    │   │   │   └── User.java                      # Lombok entity
    │   │   ├── repository/
    │   │   │   ├── UserRepository.java            # Storage abstraction (interface)
    │   │   │   └── InMemoryUserRepository.java    # ConcurrentHashMap + AtomicLong
    │   │   └── service/UserService.java           # Business logic
    │   └── resources/application.properties
    └── test/java/com/example/usermanagement
        ├── UserManagementApplicationTests.java
        ├── controller/UserControllerTest.java             # @WebMvcTest, service mocked
        ├── controller/UserControllerIntegrationTest.java  # @SpringBootTest, full stack
        ├── dto/UserRequestValidationTest.java
        ├── exception/ErrorResponseTest.java
        ├── exception/ExceptionTypesTest.java
        ├── exception/GlobalExceptionHandlerTest.java
        ├── model/GenderTest.java
        ├── model/UserTest.java
        ├── repository/InMemoryUserRepositoryTest.java
        └── service/UserServiceTest.java
```

## Architecture

The application follows a classic layered design. Each layer depends only on the layer below it, and the service depends on the `UserRepository` **interface**, not on a concrete implementation.

```
            HTTP client (curl / Swagger UI / frontend)
                              │
                              ▼
 ┌──────────────────────────────────────────────────────────┐
 │  Controller layer      UserController  (/api/users)      │
 │  - request mapping, @Valid, HTTP status codes            │
 └──────────────────────────────────────────────────────────┘
                              │  UserRequest / User
                              ▼
 ┌──────────────────────────────────────────────────────────┐
 │  Service layer         UserService                       │
 │  - business rules, UserNotFound / UserAlreadyExists      │
 └──────────────────────────────────────────────────────────┘
                              │
                              ▼
 ┌──────────────────────────────────────────────────────────┐
 │  Repository layer      «interface» UserRepository        │
 │                                 ▲                        │
 │                                 │ implements             │
 │                    InMemoryUserRepository                │
 │       ConcurrentHashMap<Long, User> + AtomicLong ids     │
 └──────────────────────────────────────────────────────────┘

 Cross-cutting: GlobalExceptionHandler (@RestControllerAdvice)
   ApiException (UserNotFound 404, UserAlreadyExists 409)
   Validation errors (body / path)  → 400 (with fieldErrors)
   Malformed JSON / bad path param  → 400
   Unknown path 404, wrong method 405, wrong content type 415
   Anything else                    → 500 (logged, no details leaked)
```

Design notes:

- **Thread safety** – `ConcurrentHashMap` handles concurrent reads/writes, and `AtomicLong` generates unique ids without locking.
- **Defensive copies** – the in-memory repository stores and returns copies of `User` objects, so callers can't change stored state by mutating a returned object (this matches how a database-backed repository behaves).
- **Request DTO** – clients send a `UserRequest` (no `id`); the server owns id assignment. Validation rules live on the DTO, so the `User` entity stays a plain data class.

## Build and Run

### Prerequisites

- JDK 21 (`java -version` should report 21)
- Maven 3.6.3 or newer (`mvn -version`)

### Build and test

```bash
cd user-management-service
mvn clean verify
```

### Run

```bash
mvn spring-boot:run
```

Or build the jar and run it:

```bash
mvn clean package
java -jar target/user-management-service-0.0.1-SNAPSHOT.jar
```

The application starts on **http://localhost:8080**. To use a different port, set `server.port` in `src/main/resources/application.properties` or pass `--server.port=9090`.

## API Reference

Base URL: `http://localhost:8080/api/users`

| Method   | Path               | Description        | Request body  | Success           | Errors     |
|----------|--------------------|--------------------|---------------|-------------------|------------|
| `GET`    | `/api/users`       | List all users     | –             | `200 OK`          | –          |
| `GET`    | `/api/users/{id}`  | Get one user       | –             | `200 OK`          | `400`, `404` |
| `POST`   | `/api/users`       | Add a user         | `UserRequest` | `201 Created` + `Location` header | `400`, `409`, `415` |
| `PUT`    | `/api/users/{id}`  | Update a user      | `UserRequest` | `200 OK`          | `400`, `404`, `409`, `415` |
| `DELETE` | `/api/users/{id}`  | Delete a user      | –             | `204 No Content`  | `400`, `404` |

First name + last name must be unique (case-insensitive). Creating a duplicate, or renaming a user to another user's name, returns `409 Conflict`.

`UserRequest` body:

```json
{
  "firstName": "Diana",
  "lastName": "Prince",
  "age": 28,
  "gender": "FEMALE"
}
```

### List all users

```bash
curl -s http://localhost:8080/api/users
```

```json
[
  { "id": 1, "firstName": "Alice",   "lastName": "Smith",   "age": 30, "gender": "FEMALE" },
  { "id": 2, "firstName": "Bob",     "lastName": "Johnson", "age": 42, "gender": "MALE" },
  { "id": 3, "firstName": "Charlie", "lastName": "Brown",   "age": 25, "gender": "OTHER" }
]
```

### Get a user by id

```bash
curl -s http://localhost:8080/api/users/2
```

```json
{ "id": 2, "firstName": "Bob", "lastName": "Johnson", "age": 42, "gender": "MALE" }
```

### Add a user

```bash
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Diana","lastName":"Prince","age":28,"gender":"FEMALE"}'
```

```
HTTP/1.1 201
Location: http://localhost:8080/api/users/4
Content-Type: application/json

{ "id": 4, "firstName": "Diana", "lastName": "Prince", "age": 28, "gender": "FEMALE" }
```

### Update a user

```bash
curl -s -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Alice","lastName":"Walker","age":31,"gender":"FEMALE"}'
```

```json
{ "id": 1, "firstName": "Alice", "lastName": "Walker", "age": 31, "gender": "FEMALE" }
```

`PUT` replaces the whole user, so every field is required.

### Delete a user

```bash
curl -i -X DELETE http://localhost:8080/api/users/3
```

```
HTTP/1.1 204
```

## Error Handling

All errors share one JSON shape, produced by `GlobalExceptionHandler`. Stack traces and exception details are never returned to the client; unexpected errors are logged server-side.

| Field         | Description                                       |
|---------------|---------------------------------------------------|
| `timestamp`   | When the error occurred (ISO-8601, UTC)           |
| `status`      | HTTP status code                                  |
| `error`       | HTTP reason phrase                                |
| `code`        | Machine-readable error code (see table below)     |
| `message`     | Human-readable description                        |
| `path`        | Request path                                      |
| `fieldErrors` | Map of field → message (validation errors only; omitted otherwise) |

### Error codes

| `code`                   | Status | When |
|--------------------------|--------|------|
| `USER_NOT_FOUND`         | 404    | No user with the given id |
| `USER_ALREADY_EXISTS`    | 409    | Another user already has this first + last name |
| `VALIDATION_FAILED`      | 400    | Body fails bean validation, or `{id}` is not positive (`fieldErrors` lists each field) |
| `MALFORMED_REQUEST`      | 400    | Body missing, not valid JSON, wrong field type, or unknown `gender` value |
| `INVALID_PARAMETER`      | 400    | Path variable can't be converted, e.g. `/api/users/abc` |
| `RESOURCE_NOT_FOUND`     | 404    | No endpoint at that path |
| `METHOD_NOT_ALLOWED`     | 405    | HTTP method not supported; the `Allow` header lists supported methods |
| `UNSUPPORTED_MEDIA_TYPE` | 415    | `Content-Type` isn't `application/json` |
| `INTERNAL_ERROR`         | 500    | Any unexpected error (generic message only) |

Application errors extend `ApiException`, which carries an `ErrorCode`. To add a new business error, add a code to `ErrorCode` and throw a subclass of `ApiException`; the handler maps it without further changes.

### 404 – user not found

```bash
curl -s http://localhost:8080/api/users/999
```

```json
{
  "timestamp": "2026-11-04T16:00:00.000Z",
  "status": 404,
  "error": "Not Found",
  "code": "USER_NOT_FOUND",
  "message": "User with id 999 not found",
  "path": "/api/users/999"
}
```

### 400 – validation failed

```bash
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"firstName":"","lastName":"Smith","age":-5}'
```

```json
{
  "timestamp": "2026-11-04T16:00:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "Validation failed",
  "path": "/api/users",
  "fieldErrors": {
    "firstName": "firstName is required",
    "age": "age must be >= 0",
    "gender": "gender is required"
  }
}
```

A non-positive id (`GET /api/users/0`) returns the same code with `"fieldErrors": {"id": "id must be a positive number"}`.

### 400 – invalid enum value

```bash
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"firstName":"A","lastName":"B","age":5,"gender":"UNKNOWN"}'
```

```json
{
  "status": 400,
  "code": "MALFORMED_REQUEST",
  "message": "Invalid value 'UNKNOWN' for field 'gender'; allowed values: [MALE, FEMALE, OTHER]"
}
```

### 409 – duplicate user

```bash
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"firstName":"alice","lastName":"smith","age":31,"gender":"FEMALE"}'
```

```json
{
  "status": 409,
  "error": "Conflict",
  "code": "USER_ALREADY_EXISTS",
  "message": "User 'alice smith' already exists"
}
```

### 405 / 415 / 500

| Request | Response |
|---------|----------|
| `DELETE /api/users` | `405`, `Allow: GET,POST`, message `HTTP method DELETE is not supported for this endpoint` |
| `POST /api/users` with `Content-Type: text/plain` | `415`, message `Content type 'text/plain' is not supported; use application/json` |
| Unexpected server error | `500`, message `An unexpected error occurred` |

(Shortened examples omit `timestamp`, `error` and `path`; real responses always include them.)

## API Documentation (Swagger / OpenAPI)

With the app running:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **OpenAPI YAML:** http://localhost:8080/v3/api-docs.yaml

The controller uses `@Tag`, `@Operation`, `@ApiResponse` and `@Parameter`, and the models use `@Schema`, so Swagger UI shows summaries, every status code, example values and the error schema. You can call each endpoint from Swagger UI with **Try it out**.

## Testing

```bash
mvn test
```

Running `mvn clean verify` runs 133 tests.

| Test class                        | Type                                  | Covers |
|-----------------------------------|---------------------------------------|--------|
| `UserManagementApplicationTests`  | `@SpringBootTest`                     | Context loads, in-memory repository is wired, OpenAPI info, Swagger UI redirect |
| `UserTest`                        | Plain JUnit                           | Lombok constructors, getters/setters, `equals`/`hashCode`, `toString` |
| `GenderTest`                      | Plain JUnit (parameterized)           | Enum values and `valueOf` |
| `UserRequestValidationTest`       | Bean Validation                       | Every constraint and its message, boundary values (age 0/150, 50-char names) |
| `InMemoryUserRepositoryTest`      | Plain JUnit                           | Seeding, sorting, id generation, no id reuse, defensive copies, case-insensitive name lookup, delete, concurrent saves |
| `UserServiceTest`                 | Mockito                               | Every service method, 404 and 409 paths, no save on failure |
| `ExceptionTypesTest`              | Plain JUnit                           | Exception messages, ids and error codes, `ErrorCode` → status mapping |
| `ErrorResponseTest`               | Jackson                               | JSON serialization, `fieldErrors` omitted when null/empty |
| `GlobalExceptionHandlerTest`      | Plain JUnit                           | Each handler method called directly: status, code, message, `Allow` header, no leaked details |
| `UserControllerTest`              | `@WebMvcTest` + mocked service        | Every endpoint and error status (400, 404, 405, 409, 415, 500) over HTTP |
| `UserControllerIntegrationTest`   | `@SpringBootTest` + MockMvc           | Full stack: CRUD, 404s, 400s, 409 duplicates, full create→update→delete lifecycle, `/v3/api-docs` |

The integration test uses `@DirtiesContext(AFTER_EACH_TEST_METHOD)` so every test starts from the three seeded users.

## Extending to MySQL

Only the repository layer has to change. `UserService` depends on the `UserRepository` interface, so controllers, services, DTOs, validation and error handling stay as they are.

### 1. Add dependencies to `pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2. Make `User` a JPA entity

```java
@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    private int age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;
}
```

### 3. Swap the repository implementation

Spring Data JPA's `JpaRepository` already provides `findAll`, `findById`, `save`, `existsById` and `deleteById` with the same signatures as `UserRepository`. The simplest option is to make the interface extend `JpaRepository` and delete the in-memory class:

```java
public interface UserRepository extends JpaRepository<User, Long> {
}
```

Then delete `InMemoryUserRepository.java` (or keep it for local runs by annotating it with `@Profile("inmemory")`).

To keep both implementations side by side, add a separate Spring Data interface plus an adapter instead:

```java
public interface JpaUserRepository extends JpaRepository<User, Long> { }

@Repository
@Profile("mysql")
@RequiredArgsConstructor
public class MySqlUserRepository implements UserRepository {
    private final JpaUserRepository jpa;

    public List<User> findAll()                 { return jpa.findAll(); }
    public Optional<User> findById(Long id)     { return jpa.findById(id); }
    public User save(User user)                 { return jpa.save(user); }
    public boolean existsById(Long id)          { return jpa.existsById(id); }
    public void deleteById(Long id)             { jpa.deleteById(id); }
}
```

and annotate `InMemoryUserRepository` with `@Profile("!mysql")`.

### 4. Configure the datasource in `application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/user_management?useSSL=false&serverTimezone=UTC
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.open-in-view=false
```

Use `ddl-auto=update` only for local development; in production, manage the schema with Flyway or Liquibase and set `ddl-auto=validate`.

### 5. Seed data and tests

- Move the sample users into `src/main/resources/data.sql` (with `spring.jpa.defer-datasource-initialization=true`) or a `CommandLineRunner`.
- For tests, use Testcontainers (`org.testcontainers:mysql`) or H2 so `mvn verify` doesn't need a local MySQL server.

A quick way to start MySQL locally:

```bash
docker run -d --name mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=secret -e MYSQL_DATABASE=user_management mysql:8.4
```
