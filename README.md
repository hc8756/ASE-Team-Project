# ASE-Team-Project
Repository for Team GLACJ's Fall 2025 Advanced Software Engineering team project.

## Overview

This project implements a Ledger Service that allows users to:
- Manage users and their transactions
- Track budgets and spending summaries
- View analytics such as weekly and monthly summaries

It is built with Spring Boot (Java), uses a Cloud SQL database, and exposes RESTful APIs for client interaction.

## Build and Run Instructions (Local)

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
Alternatively, access GCP-hosted version at https://ase-team-project-141125434285.europe-west1.run.app

## Client Program

View our client repository here: https://github.com/hc8756/ASE-Team-Project-Client

This is an example of a client for general users. It allows the user to log in or create an account before viewing their homepage. The homepage contains a list of the user's transactions which can be added to and edited by the viewer. It also shows a user analytics of their weekly and monthly budgets.

Multiple people can build and run the client locally following the instructions on the client repo README. All client instances would automatically make calls to the GCP instance hosting our service. How this was implemented can be seen in [this client script](https://github.com/hc8756/ASE-Team-Project-Client/blob/main/src/main/java/dev/ase/client/service/MockApiService.java). The handling of multiple users at once is done by GCP's Cloud Run and Cloud SQL services. As mentioned in the **Viewing Log** section, GCP's Log Explorer keeps track of the IP addresses of each endpoint call, which is how we differentiate clients.

A hypothetical second client is one for banking institutions. This client would be different in that they would have a view of multiple accounts rather than only one. They could implement this by creating a database of their users. They could then view and manage their users, user transactions, and user analytics through our GET endpoints. However, they would have limited edit access to user information and transactions.

## Multiple Client Instances

Multiple clients can connect to the service simultaneously. The service differentiates clients by:

1. **User-based isolation:** Each client creates/uses their own `userId`. All data (transactions, budgets) is scoped to that user.
2. **IP logging:** GCP's Log Explorer tracks the IP address of each API call for auditing purposes.
3. **Stateless API:** The REST API is stateless - no session is maintained between requests. Clients must include `userId` in each request.

## Viewing Log
To view client activity:
1. Open [GCP Logs Explorer](https://console.cloud.google.com/welcome?authuser=0&hl=en&project=ageless-answer-474618-u4)
2. Navigate to **Monitoring → Logs Explorer**
3. Query: `textPayload:"CLIENT_LOG:"`
   Following these instructions will show you a list of all API endpoint calls made as well as their time and the IP address of the client.

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
| `/users/{userId}/deletetransaction/{transactionId}` | GET | — → `text/plain` | `userId,transactionId` | `200 OK` `"Transaction deleted successfully!"` | `404 Not Found` `"Error: User ... not found"` or `"Error: Transaction ... not found for user ..."`, `500 Internal Server Error` `"Error: Failed to delete transaction ..."` | Browser-friendly delete |
| `/users/{userId}/budget` | GET | — → `text/html` | `userId` | `200 OK` HTML budget dashboard | `404 Not Found` (HTML body) | Renders current budget, remaining, weekly spend, and links |
| `/users/{userId}/budget` | PUT | `application/json` → `application/json` | `budgetUpdate: Map` e.g. `{"budget":123.45}` | `200 OK` budget report JSON | `404 Not Found` `{"error":"User ... not found"}` | Persists new budget via service |
| `/users/{userId}/update-budget` | POST | `application/x-www-form-urlencoded` → `text/html` | `budget` | `200 OK` HTML confirmation | `404 Not Found` (HTML body) | Browser-friendly budget update |
| `/users/{userId}/weekly-summary` | GET | — → `application/json` | `userId` | `200 OK` JSON with `username`, `weeklyTotal`, `transactionCount`, `transactions` | `404 Not Found` `{"error":"User ... not found"}` | Returns last 7 days transactions |
| `/users/{userId}/monthly-summary` | GET | — → `application/json` | `userId` | `200 OK` JSON with `summary` key | `404 Not Found` `{"error":"User ... not found"}` | Text summary produced by service |
| `/users/{userId}/budget-report` | GET | — → `application/json` | `userId` | `200 OK` budget report JSON `{totalSpent, remaining, ...}` | `404 Not Found` `{"error":"User ... not found"}` | Read-only |

#### Global Error Handling
- `NoSuchElementException` → `404 Not Found` with JSON body: `{"error":"<message>"}`
- `IllegalArgumentException` → `400 Bad Request` with JSON body: `{"error":"<message>"}`

## Testing
**Note:** The set of equivalence partitions we have defined for Unit and API testing are documented in the header
comments of our test files.

### Unit Testing
- Framework: JUnit 5
- Mocking: Mockito

To run all unit tests:
```bash
mvn test
```

### Integration Testing
- Framework: JUnit 5 with Spring Boot Test
- Database: Embedded test configuration with JdbcTemplate

Integration tests verify:
- **Internal integration:** Controller ↔ Service ↔ Models working together
- **External integration:** Service ↔ JdbcTemplate ↔ PostgreSQL database
- **Shared data:** User-Transaction foreign key relationships, budget calculations, cascade deletes

Test files:
- `RouteControllerIntegrationTests.java` - Controller-Service integration
- `MockApiServiceIntegrationTests.java` - Service-Database integration

To run all integration tests:
```bash
mvn test
```

### API Testing (Postman/Newman)
- Tool: Postman with Newman CLI for CI integration

API tests cover:
- All REST endpoints with valid and invalid equivalence partitions
- Boundary value testing (zero, negative, large values)
- Multiple client simulation
- Persistent data verification

Our CI configuration automatically run the entire collection of API tests when new pull requests
are accepted to commit to the main branch

See `api-testing.md` for detailed partition documentation.

### Test Coverage
- Tool: JaCoCo

To generate a test coverage report:
```bash
mvn jacoco:report
```

The report is accessible at `target/site/jacoco/index.html`.

For our service, we have achieved 85% branch coverage. A copy of the report `index.html` is also included in the root of this repository.


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


## Static Analysis with PMD

This project uses PMD for static code analysis to enforce consistent quality and style.

1. Ensure `pom.xml` in the root directory is updated.
2. Ensure `custom-java-rules.xml` is present in `/src/main/resources`
3. For our project, we chose to use custom, more stringent rule set than the default rules.
4. Run PMD:
```bash
mvn clean pmd:pmd
``` 
The report will be available at `/target/reports/pmd.html`
5. Can also run this command:
```bash
mvn clean site
``` 
The reports will be available at `/target/site/pmd.html` and `/target/site/cpd.html`

Copies of "before" PMD reports and clean "after" PMD reports are available in the root directory.
The most up-to-date before and after PMD reports in the root directory are `pmd_before_dec.html` and `pmd_after_dec.html`
The CPD reports were always clean, and a copy is also included in the root directory.

## Continuous Integration

This project uses GitHub Actions for CI. The pipeline runs automatically on pushes and pull requests to `main`.

### CI Pipeline Jobs

| Job | Description |
|-----|-------------|
| Unit Tests | Runs all unit tests (`*UnitTests`, `*Tests`) |
| Integration Tests | Runs database integration tests (`*IntegrationTests`) |
| API Tests | Runs Newman/Postman API tests against live application |
| Code Coverage | Generates JaCoCo coverage report |
| Static Analysis | Runs Checkstyle and PMD |

### CI Configuration

The CI workflow is defined in `.github/workflows/maven.yml`.

### Running Tests Locally
```bash
# Unit tests
mvn test -Dtest="*UnitTests,*Tests"

# Integration tests (requires PostgreSQL)
mvn test -Dtest="*IntegrationTests"

# API tests (requires application running)
newman run postman/postman_collection.json -e postman/postman_environment.json
```

## Developing a Third-Party Client

To develop your own client application that uses this service:

### 1. Base URL
- **Local:** `http://localhost:8080`
- **Production:** `https://ase-team-project-141125434285.europe-west1.run.app`

### 2. Required Request Flow
1. **Create a user first** via `POST /users` (JSON) or `POST /users/form` (HTML)
2. Store the returned `userId` (UUID) for subsequent requests
3. All `/users/{userId}/...` endpoints require a valid `userId`
4. Transaction endpoints require the transaction to belong to the specified user

### 3. Content Types
- **JSON endpoints:** Set `Content-Type: application/json` header
- **Form endpoints:** Set `Content-Type: application/x-www-form-urlencoded` header

### 4. Example: Creating a User (JSON)
```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username": "john_doe", "email": "john@example.com", "budget": 1000.00}'
```

Response:
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john_doe",
  "email": "john@example.com",
  "budget": 1000.00
}
```

### 5. Example: Creating a Transaction
```bash
curl -X POST http://localhost:8080/users/{userId}/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "Groceries", "amount": 50.00, "category": "FOOD"}'
```

### 6. Some Examples Of Valid Transaction Categories
`FOOD`, `TRANSPORTATION`, `ENTERTAINMENT`, `UTILITIES`, `HEALTHCARE`, `OTHER`

### 7. Error Handling
- `400 Bad Request`: Invalid input (check `error` field in response)
- `404 Not Found`: User or transaction not found
- `500 Internal Server Error`: Server-side error

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
- **Postman/Newman (API Testing):** https://learning.postman.com/docs/collections/using-newman-cli/command-line-integration-with-newman/

ChatGPT assisted in referencing documentation, fixing style warnings (long lines, import order), and minor debugging.

We used **GitHub Issues** to track progress and assign tasks.

Individual member contributions can be viewed under the **Issues** section of this repository.

All team members participated in reviewing and merging pull requests to ensure code quality and consistency.
