# ASE-Team-Project
Repository for Team GLACJ's Fall 2025 Advanced Software Engineering team project.

## Overview

This project implements a Ledger Service that allows users to:
- Manage users and their transactions
  Track budgets and spending summaries
  View analytics such as weekly and monthly summaries

It is built with Spring Boot (Java), uses a Cloud SQL database, and exposes RESTful APIs for client interaction.

## Build and Run Instructions

1. Prerequisites
- Java 17+
- Maven 3.9+
- Spring Boot 3.4.4
- JaCoCo Maven Plugin 0.8.11
- Maven Checkstyle Plugin 3.2.0
- Google Cloud SQL instance (configured for this project)

2. Database setup
Make sure the SQL instance running our database is active before starting the application.
- Go to [Google Cloud Console](https://console.cloud.google.com/welcome/new?authuser=1&project=ase-group-project-474618)
- Log in/create account with your Columbia email
- Open **Cloud SQL → Instances**
- Select **ase-project-db**
- Click **Start** or **Stop** as needed

3. Run the application
```bash
mvn compile
mvn spring-boot:run
```
Once started, navigate to http://127.0.0.1:8080 or http://localhost:8080/ in your web browser.

## API Documentation

### Summary of Endpoints

> **Call order constraint:** Create a user **first** (`POST /users` or `POST /users/form`).  
> All `/users/{userId}/...` routes require an existing user.  
> Transaction routes additionally require the transaction to belong to `userId`.

| Endpoint | Method | Consumes → Produces | Inputs | Success (2xx) | Errors (4xx/5xx) | Side-effects / Notes |
|---|---|---|---|---|---|---|
| `/` or `/index` | GET | — → `text/html` | — | `200 OK` HTML page listing users | — | Renders simple HTML with links to users. |
| `/users` | GET | — → `application/json` | — | `200 OK` `List<User>` | — | None |
| `/users/{userId}` | GET | — → `application/json` | `userId: UUID` | `200 OK` `User` | `404 Not Found` `{"error":"User ... not found"}` | None |
| `/users` | POST | `application/json` → `application/json` | `User` JSON `{username,email,budget,...}` | `201 Created` `User` | `400 Bad Request` `{"error":...}` if `IllegalArgumentException` thrown by service | Creates user |
| `/users/form` | POST | `application/x-www-form-urlencoded` → `text/html` | `username,email,budget` form fields | `201 Created` HTML confirmation | `400 Bad Request` HTML if service throws `IllegalArgumentException` | Creates user; browser-friendly |
| `/users/{userId}` | PUT | `application/json` → `application/json` | `userId`, `User` JSON | `200 OK` updated `User` | `404 Not Found` `{"error":"User ... not found"}` | **Deletes then recreates** the user in service, then reassigns same `userId`. Be aware of potential side-effects on related data in the service layer. |
| `/users/{userId}/update-form` | POST | `application/x-www-form-urlencoded` → `text/html` | `username,email,budget` | `200 OK` HTML confirmation | `404 Not Found` (HTML body) if user missing | Same delete-and-recreate behavior as JSON PUT. |
| `/users/create-form` | GET | — → `text/html` | — | `200 OK` HTML form | — | Browser-only helper |
| `/users/{userId}/edit-form` | GET | — → `text/html` | `userId` | `200 OK` HTML form prefilled | `404 Not Found` (HTML body) | Browser-only helper |
| `/users/{userId}` | DELETE | — → `application/json` | `userId` | `200 OK` `{"deleted":true,"userId":...}` | `404 Not Found` `{"error":"User ... not found"}` | Deletes user |
| `/deleteuser/{userId}` | GET | — → `text/plain` | `userId` | `200 OK` `"User deleted successfully"` | `404 Not Found` `{"error":"User ... not found"}` (via exception) | Browser-friendly delete |
| `/users/{userId}/transactions` | GET | — → `application/json` | `userId` | `200 OK` `List<Transaction>` | `404 Not Found` `"Error: User ... not found"` (plain string), `500 Internal Server Error` `"Error retrieving transactions: ..."` | None |
| `/users/{userId}/transactions/{transactionId}` | GET | — → `application/json` | `userId`,`transactionId` | `200 OK` `Transaction` | `404 Not Found` `{"error":"Transaction ... not found for user ..."}` or `{"error":"User ... not found"}` | Requires that `transaction.userId == userId` |
| `/users/{userId}/transactions` | POST | `application/json` → `application/json` | `userId`, `Transaction` JSON | `201 Created` `Transaction` | `404 Not Found` `{"error":"User ... not found"}` | Server sets `transaction.userId = userId` before save |
| `/users/{userId}/transactions/form` | POST | `application/x-www-form-urlencoded` → `text/html` | `description,amount,category` | `201 Created` HTML confirmation | `404 Not Found` `"Error: User ... not found"` (plain string), `500 Internal Server Error` `"Error creating transaction: ..."` | Browser-friendly create |
| `/users/{userId}/transactions/create-form` | GET | — → `text/html` | `userId` | `200 OK` HTML form | `404 Not Found` (HTML body) | Browser-only helper |
| `/users/{userId}/transactions/{transactionId}` | PUT | `application/json` → `application/json` | `updates: Map<String,Object>` | `200 OK` updated `Transaction` | `404 Not Found` `{"error":"User ... not found"}` or `{"error":"Transaction ... not found for user ..."}` or `{"error":"Transaction ... not found"}` | Partial update keys handled by service |
| `/users/{userId}/transactions/{transactionId}` | DELETE | — → `application/json` | `userId,transactionId` | `200 OK` `{"deleted":true,"userId":...,"transactionId":...}` | `404 Not Found` `{"error":"User ... not found"}` or `{"error":"Transaction ... not found for user ..."}` or `{"error":"Transaction ... not found"}` | Deletes transaction |
| `/users/{userId}/deletetransaction/{transactionId}` | GET | — → `text/plain` | `userId,transactionId` | `200 OK` `"Transaction deleted successfully!"` | `200 OK` `"Error: User ... not found"` or `"Error: Transaction ... not found for user ..."` or `"Error: Failed to delete transaction ..."` (plain strings, not HTTP errors) | Browser-friendly delete; returns 200 with error text on failures |
| `/users/{userId}/budget` | GET | — → `text/html` | `userId` | `200 OK` HTML budget dashboard | `404 Not Found` (HTML body) | Renders current budget, remaining, weekly spend, and links |
| `/users/{userId}/budget` | PUT | `application/json` → `application/json` | `budgetUpdate: Map` e.g. `{"budget":123.45}` | `200 OK` budget report JSON | `404 Not Found` `{"error":"User ... not found"}` | Persists new budget via service |
| `/users/{userId}/update-budget` | POST | `application/x-www-form-urlencoded` → `text/html` | `budget` | `200 OK` HTML confirmation | `404 Not Found` (HTML body) | Browser-friendly budget update |
| `/users/{userId}/weekly-summary` | GET | — → `text/html` | `userId` | `200 OK` HTML with table and total | `404 Not Found` (HTML body) | Renders last 7 days transactions |
| `/users/{userId}/monthly-summary` | GET | — → `text/html` | `userId` | `200 OK` HTML `<pre>` summary | `404 Not Found` (HTML body) | Text summary produced by service |
| `/users/{userId}/budget-report` | GET | — → `application/json` | `userId` | `200 OK` budget report JSON `{totalSpent, remaining, ...}` | `404 Not Found` `{"error":"User ... not found"}` | Read-only |

#### Global Error Handling
- `NoSuchElementException` → `404 Not Found` with JSON body: `{"error":"<message>"}`
- `IllegalArgumentException` → `400 Bad Request` with JSON body: `{"error":"<message>"}`

## Testing

### Unit Testing
- Framework: JUnit 5
- Mocking: Mockito

To run all unit tests:
```bash
mvn test
```

### Endpoint Testing

Please see `api-testing.md` in the root of this repository for detailed instructions on running tests with `curl`.

### Test Coverage
- Tool: JaCoCo

To generate a test coverage report:
```bash
mvn jacoco:report
```

The report is accessible at `target/site/jacoco/index.html`.

For the first iteration, we acheived 56% branch coverage. A copy of the report `index.html` is also included in the root of this repository.


## Style Checker

We use the **Maven Checkstyle Plugin (v3.2.0)** to automatically verify code formatting and style compliance based on the Google Java Style Guide.

This helps ensure readability and uniform coding practices across all contributors.

To check for style violations:
```bash
mvn checkstyle:check
```

To generate a checkstyle report:
```bash
mvn checkstyle:checkstyle
```

The report is accessible at `target/site/checkstyle.html`. A copy of a clean report `checkstyle.html` is also included in the root of this repository.

## Documentation and Organization

All source files are documented using Javadoc and inline comments explaining implementation details where relevant.

Both JaCoCo and Checkstyle reports are included in the root directory of the repository.

Official documentation and reference materials used throughout the project include:
- **Java SE 17:** https://docs.oracle.com/en/java/javase/17/docs/api/
- **Apache Maven:** https://maven.apache.org/guides/index.html
- **Spring Boot Framework:** https://docs.spring.io/spring-boot/docs/current/reference/html/
- **JUnit 5:** https://junit.org/junit5/docs/current/user-guide/
- **Mockito Framework:** https://site.mockito.org/
- **JaCoCo Code Coverage Library:** https://www.jacoco.org/jacoco/trunk/doc/
- **Maven Checkstyle Plugin:** https://maven.apache.org/plugins/maven-checkstyle-plugin/
- **Google Java Style Guide (Checkstyle Rules):** https://google.github.io/styleguide/javaguide.html
- **Spring Testing (MockMvc):** https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-mvc-test-framework

ChatGPT assisted in referencing documentation, fixing style warnings (long lines, import order), and minor debugging.

We used **GitHub Issues** to track progress and assign tasks.

Individual member contributions can be viewed under the **Issues** section of this repository.

All team members participated in reviewing and merging pull requests to ensure code quality and consistency.