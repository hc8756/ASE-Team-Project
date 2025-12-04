# API Equivalence Partitions Documentation

This document defines the equivalence partitions for each API endpoint in the Personal Finance Tracker service. Each partition represents a class of inputs that should be treated equivalently by the system.

## Table of Contents
[Index/Home Endpoints](#indexhome-endpoints)  
[User Management - GET /users](#user-management---get-users)  
[User Management - POST /users (JSON)](#user-management---post-users-json)  
[User Management - GET /users/{userId}](#user-management---get-usersuserid)  
[User Management - PUT /users/{userId}](#user-management---put-usersuserid)  
[User Management - POST /users/form (HTML)](#user-management---post-usersform-html)  
[User Management - POST /users/{userId}/update-form (HTML)](#user-management---post-usersuseridupdate-form-html)  
[User Management - GET Forms](#user-management---get-forms)  
[User Management - DELETE /users/{userId}](#user-management---delete-usersuserid)  
[User Management - GET /deleteuser/{userId}](#user-management---get-deleteuseruserid)  
[Transaction Management - POST](#transaction-management---post-transactions)  
[Transaction Management - GET List](#transaction-management---get-transactions-list)  
[Transaction Management - GET Single](#transaction-management---get-single-transaction)  
[Transaction Management - PUT](#transaction-management---put-transaction)  
[Transaction Management - DELETE](#transaction-management---delete-transaction)  
[Transaction Management - GET /deletetransaction](#transaction-management---get-deletetransaction)  
[Transaction Management - Forms](#transaction-management---forms)  
[Budget Management - GET Page](#budget-management---get-budget-page)  
[Budget Management - PUT /budget](#budget-management---put-budget-json)  
[Budget Management - POST /update-budget](#budget-management---post-update-budget-form)  
[Analytics - Weekly Summary](#analytics---weekly-summary)  
[Analytics - Monthly Summary](#analytics---monthly-summary)  
[Analytics - Budget Report](#analytics---budget-report)

---

## Index/Home Endpoints

### GET / and GET /index

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Valid GET request to `/` | 200 OK, HTML with welcome message | `GET / - Home Page (Valid)` |
| P2 | Valid | Valid GET request to `/index` | 200 OK, HTML with welcome message | `GET /index - Index Page (Valid)` |
| P3 | Invalid | Invalid path | 404 Not Found | `GET /invalid-path - Invalid Path` |

---

## User Management - GET /users

### GET /users

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Valid GET request | 200 OK, JSON array of users | `GET /users - List All Users (Valid)` |

---

## User Management - POST /users (JSON)

### POST /users

**Input Parameters:**
- `username` (String, required)
- `email` (String, required)
- `budget` (Double, required, >= 0)

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | All fields valid (username, email, positive budget) | 201 Created with user data | `POST /users - Create Valid User` |
| P2 | Valid/Boundary | budget = 0 (minimum valid) | 201 Created | `POST /users - Zero Budget` |
| P3 | Valid/Boundary | budget = 99999999.99 (high value) | 201 Created | `POST /users - Large Budget` |
| P4 | Invalid | username = null | 400 Bad Request | `POST /users - Missing Username` |
| P5 | Invalid | email = null | 400 Bad Request | `POST /users - Missing Email` |
| P6 | Invalid | Malformed JSON | 400 Bad Request | `POST /users - Malformed JSON` |
| P7 | Invalid | Duplicate username | 400 Bad Request | `POST /users - Duplicate Username` |
| P8 | Invalid | Duplicate email | 400 Bad Request | `POST /users - Duplicate Email` |

---

## User Management - GET /users/{userId}

### GET /users/{userId}

**Input Parameters:**
- `userId` (UUID, path parameter)

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Existing userId | 200 OK with user data | `GET /users/{userId} - Existing User` |
| P2 | Invalid | Non-existent but valid UUID format | 404 Not Found | `GET /users/{userId} - Non-existent UUID` |
| P3 | Invalid | Invalid UUID format | 400 Bad Request | `GET /users/{userId} - Invalid UUID Format` |

---

## User Management - PUT /users/{userId}

### PUT /users/{userId}

**Input Parameters:**
- `userId` (UUID, path parameter)
- `username` (String, body)
- `email` (String, body)
- `budget` (Double, body)

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Existing user, valid update data | 200 OK with updated user | `PUT /users/{userId} - Update All Fields` |
| P2 | Invalid | Non-existent userId | 404 Not Found | `PUT /users/{userId} - Non-existent User` |
| P3 | Invalid | Duplicate username (conflict with other user) | 400 Bad Request | `PUT /users/{userId} - Duplicate Username` |
| P4 | Invalid | Duplicate email (conflict with other user) | 400 Bad Request | `PUT /users/{userId} - Duplicate Email` |
| P5 | Valid/Boundary | budget = 0 | 200 OK | `PUT /users/{userId} - Zero Budget` |

---

## User Management - POST /users/form (HTML)

### POST /users/form

**Input Parameters:**
- `username` (form field)
- `email` (form field)
- `budget` (form field)

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | All fields valid | 201 Created, HTML success page | `POST /users/form - Valid Form Data` |
| P2 | Invalid | Duplicate username | 400 Bad Request, HTML error | `POST /users/form - Duplicate Username` |
| P3 | Invalid | Duplicate email | 400 Bad Request, HTML error | `POST /users/form - Duplicate Email` |

---

## User Management - POST /users/{userId}/update-form (HTML)

### POST /users/{userId}/update-form

**Input Parameters:**
- `userId` (UUID, path parameter)
- `username` (form field)
- `email` (form field)
- `budget` (form field)

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Existing user, valid data | 200 OK, HTML success page | `POST /users/{userId}/update-form - Valid` |
| P2 | Invalid | Non-existent userId | 404 Not Found | `POST /users/{userId}/update-form - User Not Found` |
| P3 | Invalid | Duplicate username | 400 Bad Request | `POST /users/{userId}/update-form - Duplicate Username` |
| P4 | Invalid | Duplicate email | 400 Bad Request | `POST /users/{userId}/update-form - Duplicate Email` |

---

## User Management - GET Forms

### GET /users/create-form

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Valid GET request | 200 OK, HTML form | `GET /users/create-form` |

### GET /users/{userId}/edit-form

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Existing userId | 200 OK, HTML form with user data | `GET /users/{userId}/edit-form - Existing User` |
| P2 | Invalid | Non-existent userId | 404 Not Found | `GET /users/{userId}/edit-form - Non-existent User` |

---

## Transaction Management - POST Transactions

### POST /users/{userId}/transactions

**Input Parameters:**
- `userId` (UUID, path parameter)
- `description` (String, required, non-empty)
- `amount` (Double, required, > 0)
- `category` (String, required, valid enum value)

**Valid Categories:** FOOD, TRANSPORTATION, ENTERTAINMENT, UTILITIES, SHOPPING, HEALTHCARE, TRAVEL, EDUCATION, OTHER

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | All fields valid | 201 Created with transaction data | `POST Transaction - Valid` |
| P2 | Invalid | Non-existent userId | 404 Not Found | `POST Transaction - Non-existent User` |
| P3 | Invalid | Invalid category | 400/500 Error | `POST Transaction - Invalid Category` |
| P4 | Invalid/Boundary | amount < 0 (negative) | 400/500 Error | `POST Transaction - Negative Amount` |
| P5 | Invalid/Boundary | amount = 0 | 400/500 Error | `POST Transaction - Zero Amount` |
| P6 | Invalid | description = "" (empty) | 400/500 Error | `POST Transaction - Empty Description` |
| P7 | Valid | Large amount (boundary high) | 201 Created | `POST Transaction - Large Amount` |

---

## Transaction Management - GET Transactions List

### GET /users/{userId}/transactions

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Existing userId | 200 OK, JSON array | `GET Transactions - Valid User` |
| P2 | Invalid | Non-existent userId | 404 Not Found | `GET Transactions - Non-existent User` |

---

## Transaction Management - GET Single Transaction

### GET /users/{userId}/transactions/{transactionId}

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Valid userId and transactionId | 200 OK with transaction | `GET Single Transaction - Valid` |
| P2 | Invalid | Transaction belongs to different user | 404 Not Found | `GET Single Transaction - Wrong User` |
| P3 | Invalid | Non-existent transactionId | 404 Not Found | `GET Single Transaction - Non-existent Transaction` |

---

## Transaction Management - PUT Transaction

### PUT /users/{userId}/transactions/{transactionId}

**Input Parameters:**
- `userId` (UUID, path parameter)
- `transactionId` (UUID, path parameter)
- `amount` (Double, optional)
- `description` (String, optional)
- `category` (String, optional)

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Valid userId, transactionId, update data | 200 OK with updated transaction | `PUT Transaction - Update Amount` |
| P2 | Invalid | Non-existent userId | 404 Not Found | `PUT Transaction - Non-existent User` |
| P3 | Invalid | Transaction belongs to different user | 404 Not Found | `PUT Transaction - Wrong User` |
| P4 | Invalid | Non-existent transactionId | 404 Not Found | `PUT Transaction - Non-existent Transaction` |
| P5 | Invalid | Invalid category in update | 400 Bad Request | `PUT Transaction - Invalid Category` |

---

## Transaction Management - Forms

### GET /users/{userId}/transactions/create-form

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Existing userId | 200 OK, HTML form | `GET /transactions/create-form - Valid` |
| P2 | Invalid | Non-existent userId | 404 Not Found | `GET /transactions/create-form - User Not Found` |

### POST /users/{userId}/transactions/form

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Valid form data | 201 Created, HTML success | `POST /transactions/form - Valid` |
| P2 | Invalid | Non-existent userId | 404 Not Found | `POST /transactions/form - User Not Found` |
| P3 | Invalid | Invalid category | 500 Error | `POST /transactions/form - Invalid Category` |

---

## Budget Management - GET Budget Page

### GET /users/{userId}/budget

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Existing userId | 200 OK, HTML budget page | `GET Budget Page - Valid` |
| P2 | Invalid | Non-existent userId | 404 Not Found | `GET Budget Page - Non-existent User` |

---

## Budget Management - PUT /budget (JSON)

### PUT /users/{userId}/budget

**Input Parameters:**
- `userId` (UUID, path parameter)
- `budget` (Double, body, >= 0)

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Existing user, valid budget | 200 OK with budget report | `PUT Budget - Update` |
| P2 | Valid/Boundary | budget = 0 | 200 OK | `PUT Budget - Zero Budget` |
| P3 | Invalid/Boundary | budget < 0 (negative) | 400/500 Error | `PUT Budget - Negative` |
| P4 | Invalid | Non-existent userId | 404 Not Found | `PUT Budget - Non-existent User` |

---

## Budget Management - POST /update-budget (Form)

### POST /users/{userId}/update-budget

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Existing user, valid budget | 200 OK, HTML success | `POST Update Budget Form - Valid` |
| P2 | Invalid | Non-existent userId | 404 Not Found | `POST Update Budget Form - User Not Found` |

---

## Analytics - Weekly Summary

### GET /users/{userId}/weekly-summary

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Existing userId | 200 OK, JSON summary | `GET Weekly Summary - Valid` |
| P2 | Invalid | Non-existent userId | 404 Not Found | `GET Weekly Summary - Non-existent User` |

---

## Analytics - Monthly Summary

### GET /users/{userId}/monthly-summary

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Existing userId | 200 OK, JSON with summary | `GET Monthly Summary - Valid` |
| P2 | Invalid | Non-existent userId | 404 Not Found | `GET Monthly Summary - Non-existent User` |

---

## Analytics - Budget Report

### GET /users/{userId}/budget-report

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Existing userId | 200 OK, JSON report | `GET Budget Report - Valid` |
| P2 | Invalid | Non-existent userId | 404 Not Found | `GET Budget Report - Non-existent User` |

---

## Transaction Management - DELETE Transaction

### DELETE /users/{userId}/transactions/{transactionId}

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Valid userId and transactionId | 200 OK, deletion confirmed | `DELETE Transaction - Valid` |
| P2 | Invalid | Already deleted transaction | 404 Not Found | `DELETE Transaction - Already Deleted` |
| P3 | Invalid | Transaction belongs to different user | 404 Not Found | `DELETE Transaction - Wrong User` |

---

## Transaction Management - GET /deletetransaction

### GET /users/{userId}/deletetransaction/{transactionId}

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Valid userId and transactionId | 200 OK, success message | `GET /deletetransaction - Valid` |
| P2 | Invalid | Non-existent or wrong user | Error message | `GET /deletetransaction - Invalid` |

## User Management - DELETE /users/{userId}

### DELETE /users/{userId}

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Existing userId | 200 OK, deletion confirmed | `DELETE /users/{userId} - Valid` |
| P2 | Invalid | Already deleted user | 404 Not Found | `DELETE /users/{userId} - Already Deleted` |
| P3 | Invalid | Non-existent UUID | 404 Not Found | `DELETE /users/{userId} - Non-existent UUID` |
| P4 | Invalid | Invalid UUID format | 400 Bad Request | `DELETE /users/{userId} - Invalid UUID Format` |

---

## User Management - GET /deleteuser/{userId}

### GET /deleteuser/{userId}

| Partition | Type | Input | Expected Output | Test Coverage |
|-----------|------|-------|-----------------|---------------|
| P1 | Valid | Existing userId | 200 OK, success message | `GET /deleteuser/{userId} - Valid` |
| P2 | Invalid | Non-existent userId | 404 Not Found | `GET /deleteuser/{userId} - Not Found` |

---

## Boundary Value Analysis Summary

| Input | Lower Boundary | Upper Boundary |
|-------|----------------|----------------|
| User budget | 0 (valid) | 99999999.99 (valid) |
| Transaction amount | 0 (invalid), 0.01 (valid) | Large value (valid) |
| UUID format | Valid UUID | Invalid string |
| String fields | Empty string (invalid) | Non-empty (valid) |

---

## Multiple Clients Testing

The test suite exercises multiple clients through:
- Creating two test users (`testUserId` and `testUserId2`)
- Testing transaction ownership validation (accessing another user's transactions)
- Testing concurrent data isolation

## Persistent Data Testing

The test suite verifies data persistence through:
- Create → Retrieve sequences
- Update → Retrieve sequences
- Delete → Verify deletion sequences
- Cascade delete verification (user deletion removes transactions)

## CI Integration

These tests run automatically via Newman in the CI pipeline (./github/workflows/maven.yml).