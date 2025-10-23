# API Testing with curl

For setup and run instructions, please see `README.md` in the root of this repository.

You can test every endpoint in this application using `curl`. By default, the server runs at `http://127.0.0.1:8080` or `http://localhost:8080`. These examples use `http://127.0.0.1:8080`. `{UIUD_VALID} and {UIUD_INVALID} refer to user/transaction IDs and are placeholder for real values.

Note: For more visually appealing formatting, we use **Lynx** and **httpie**. If you want this too, install these tools.

Mac:
```bash
brew install lynx
brew install httpie
```
Windows:
```bash
sudo apt install lynx
sudo apt install httpie
```
(None of us use Windows so hopefully this works)

---

## GET `/` or `/index` — homepage (success only)

### Success
```bash
curl -isS http://127.0.0.1:8080/
curl -isS http://127.0.0.1:8080/index

# nice formatting
curl -isS http://127.0.0.1:8080/ | lynx -dump -stdin -nolist
curl -isS http://127.0.0.1:8080/index | lynx -dump -stdin -nolist
```

- Sample output:
  ```bash
  HTTP/1.1 200
  Content-Type: text/html
  Content-Length: 318
  Date: Thu, 23 Oct 2025 00:09:54 GMT

          Welcome to the Personal Finance Tracker

  Existing Users:
  - user1 | user1@gmail.com | Budget: $600.00
  - user2 | user2@columbia.edu | Budget: $1100.00
  ```

- Failure  
  N/A (endpoint always 200)

---

## GET `/users` — list all users (success only)

### Success
```bash
curl -isS http://127.0.0.1:8080/users

# nice formatting
http GET http://127.0.0.1:8080/users
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Connection: keep-alive
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 00:22:38 GMT
  Keep-Alive: timeout=60
  Transfer-Encoding: chunked

  [
    {
      "budget": 600.0,
      "email": "user1@gmail.com",
      "userId": "{UUID_VALID}",
      "username": "user1"
    },
    {
      "budget": 1100.0,
      "email": "user2@columbia.edu",
      "userId": "{UUID_VALID}",
      "username": "user2"
    }
  ]
  ```

- Failure  
  N/A (endpoint always 200)

---

## GET `/users/{userId}` — get a user

### Success
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}

# nice formatting
http GET http://127.0.0.1:8080/users/{UUID_VALID}
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Connection: keep-alive
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 00:26:17 GMT
  Keep-Alive: timeout=60
  Transfer-Encoding: chunked

  {
    "budget": 500.0,
    "email": "user@gmail.com",
    "userId": "{UUID_VALID}",
    "username": "user"
  }
  ```

### Failure (invalid or not found)
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_INVALID}
```
- Sample output:
  ```bash
  HTTP/1.1 400
  Content-Type: application/json
  Transfer-Encoding: chunked
  Date: Thu, 23 Oct 2025 05:03:57 GMT
  Connection: close

  {"error":"Invalid UUID string: {UUID_INVALID}"}
  ```

---

## POST `/users` (JSON) — create user

### Success
```bash
curl -isS -X POST http://127.0.0.1:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username":"user","email":"user@email.com","budget":200}'

# nice formatting
http POST http://127.0.0.1:8080/users \
  username=user \
  email=user@email.com \
  budget:=200
```
- Sample output:
  ```bash
  HTTP/1.1 201
  Connection: keep-alive
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 00:37:41 GMT
  Keep-Alive: timeout=60
  Transfer-Encoding: chunked

  {
    "budget": 200.0,
    "email": "user@email.com",
    "userId": "{UUID_VALID}",
    "username": "user"
  }
  ```

### Failure (duplicate username)
Run one of the above commands twice.
- Sample output:
  ```bash
  HTTP/1.1 500
  Connection: close
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 00:35:28 GMT
  Transfer-Encoding: chunked

  {
    "error": "Internal Server Error",
    "path": "/users",
    "status": 500,
    "timestamp": "2025-10-23T00:35:28.208+00:00"
  }
  ```

---

## POST `/users/form` (form) — create user via HTML form

### Success
```bash
curl -isS -X POST http://127.0.0.1:8080/users/form \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=user&email=user@email.com&budget=200"

# nice formatting
http POST http://127.0.0.1:8080/users \
  username=user \
  email=user@email.com \
  budget:=200
```
- Sample output:
  ```bash
  HTTP/1.1 201
  Content-Type: text/html;charset=UTF-8
  Content-Length: 256
  Date: Thu, 23 Oct 2025 01:02:09 GMT

  User Created Successfully!

    User ID: {UUID_VALID}
    Username: user
    Email: user@email.com
    Budget: 500.0
  ```

### Failure (duplicate username)
- Sample output:
  ```bash
  HTTP/1.1 500
  Connection: close
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 03:27:03 GMT
  Transfer-Encoding: chunked

  {
    "error": "Internal Server Error",
    "path": "/users",
    "status": 500,
    "timestamp": "2025-10-23T03:27:03.092+00:00"
  }
  ```

---

## PUT `/users/{userId}` (JSON) — update user

### Success
```bash
curl -isS -X PUT http://127.0.0.1:8080/users/{UUID_VALID} \
  -H "Content-Type: application/json" \
  -d '{"username":"user","email":"user@email.com","budget":500.0}'

# nice formatting
http PUT http://127.0.0.1:8080/users/{UUID_VALID} \
  username=user \
  email=user@email.com \
  budget:=500
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Connection: keep-alive
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 03:31:43 GMT
  Keep-Alive: timeout=60
  Transfer-Encoding: chunked

  {
    "budget": 500.0,
    "email": "user@email.com",
    "userId": "{UUID_VALID}",
    "username": "user"
  }
  ```

### Failure (invalid or not found)
```bash
curl -isS -X PUT http://127.0.0.1:8080/users/{UUID_INVALID} \
  -H "Content-Type: application/json" \
  -d '{"username":"user","email":"user@email.com","budget":500.0}'
```
- Sample output:
  ```bash
  HTTP/1.1 404
  Connection: keep-alive
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 03:31:18 GMT
  Keep-Alive: timeout=60
  Transfer-Encoding: chunked

  {"error":"User {UUID_INVALID} not found"}
  ```

---

## POST `/users/{userId}/update-form` (form) — update user via HTML form

### Success
```bash
curl -isS -X POST http://127.0.0.1:8080/users/{UUID_VALID}/update-form \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=user&email=user@user.com&budget=200"

# nice formatting
curl -isS -X POST http://127.0.0.1:8080/users/{UUID_VALID}/update-form \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=user&email=user@user.com&budget=200" | lynx -dump -stdin -nolist
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Content-Type: text/html;charset=UTF-8
  Content-Length: 251
  Date: Thu, 23 Oct 2025 03:37:50 GMT

  User Updated Successfully!

    User ID: {UUID_VALID}
    Username: user
    Email: user@user.com
    Budget: $200.00
  ```

### Failure (invalid or not found)
```bash
curl -isS -X POST http://127.0.0.1:8080/users/{UUID_INVALID}/update-form \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=user&email=user@user.com&budget=200"
```
- Sample output:
  ```bash
  HTTP/1.1 404
  Content-Type: application/json
  Transfer-Encoding: chunked
  Date: Thu, 23 Oct 2025 03:38:22 GMT

  {"error":"User {UUID_INVALID} not found"}
  ```

---

## GET `/users/create-form` — serve create-user HTML form

### Success
```bash
curl -isS http://127.0.0.1:8080/users/create-form

# nice formatting
curl -isS http://127.0.0.1:8080/users/create-form | lynx -dump -stdin -nolist
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Content-Type: text/html;charset=UTF-8
  Content-Length: 323
  Date: Thu, 23 Oct 2025 03:39:44 GMT

  Create New User

    Username: ____________________
    Email: ____________________
    Budget: ____________________
    Create User
  ```

- Failure  
  N/A

---

## GET `/users/{userId}/edit-form` — serve edit form

### Success
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/edit-form

# nice formatting
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/edit-form | lynx -dump -stdin -nolist
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Content-Type: text/html;charset=UTF-8
  Content-Length: 417
  Date: Thu, 23 Oct 2025 03:41:19 GMT

  Edit User

    Username: user________________
    Email: user@user.com_______
    Budget: 200.0_______________
    Update User
  ```

### Failure (invalid)
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_INVALID}/edit-form
```
- Sample output:
  ```bash
  HTTP/1.1 400
  Content-Type: application/json
  Transfer-Encoding: chunked
  Date: Thu, 23 Oct 2025 03:42:43 GMT
  Connection: close

  {"error":"Invalid UUID string: {UUID_INVALID}"}
  ```

---

## DELETE `/users/{userId}` — delete user

### Success
```bash
curl -isS -X DELETE http://127.0.0.1:8080/users/{UUID_VALID}

# nice formatting
http DELETE http://127.0.0.1:8080/users/{UUID_VALID}
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Connection: keep-alive
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 03:18:35 GMT
  Keep-Alive: timeout=60
  Transfer-Encoding: chunked

  {
    "deleted": true,
    "userId": "{UUID_VALID}"
  }
  ```

### Failure (invalid or not found)
```bash
curl -isS -X DELETE http://127.0.0.1:8080/users/{UUID_INVALID}
```
- Sample output:
  ```bash
  HTTP/1.1 404
  Connection: keep-alive
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 03:19:40 GMT
  Keep-Alive: timeout=60
  Transfer-Encoding: chunked

  {"error":"User {UUID_INVALID} not found"}
  ```

---

## GET `/deleteuser/{userId}` — delete user via GET

### Success
```bash
curl -isS http://127.0.0.1:8080/deleteuser/{UUID_VALID}
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Content-Type: text/plain;charset=UTF-8
  Content-Length: 25
  Date: Thu, 23 Oct 2025 04:28:50 GMT

  User deleted successfully
  ```

### Failure (invalid or not found)
```bash
curl -isS http://127.0.0.1:8080/deleteuser/{UUID_INVALID}
```
- Sample output:
  ```bash
  HTTP/1.1 404
  Content-Type: application/json
  Transfer-Encoding: chunked
  Date: Thu, 23 Oct 2025 04:29:46 GMT

  {"error":"User {UUID_INVALID} not found"}
  ```

---

## GET `/users/{userId}/transactions` — list transactions

### Success
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/transactions

# nice formatting
http GET http://127.0.0.1:8080/users/{UUID_VALID}/transactions
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Connection: keep-alive
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 04:53:41 GMT
  Keep-Alive: timeout=60
  Transfer-Encoding: chunked

  [
    {
      "amount": 1000.00,
      "category": "RENT",
      "date": "2025-10-23",
      "description": "Monthly rent",
      "timestamp": "2025-10-23T00:52:11.085968",
      "transactionId": "{UUID_VALID}",
      "userId": "{UUID_VALID}"
    },
    {
      "amount": 20.00,
      "category": "FOOD",
      "date": "2025-10-23",
      "description": "Sandwich",
      "timestamp": "2025-10-23T00:49:52.307021",
      "transactionId": "{UUID_VALID}",
      "userId": "{UUID_VALID}"
    },
    {
      "amount": 50,
      "category": "GROCERIES",
      "date": "2025-10-23",
      "description": "Whole Foods",
      "timestamp": "2025-10-23T00:35:45.953535",
      "transactionId": "{UUID_VALID}",
      "userId": "{UUID_VALID}"
    }
  ]
  ```

### Failure (invalid)
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_INVALID}/transactions
```
- Sample output:
  ```bash
  HTTP/1.1 400
  Content-Type: application/json
  Transfer-Encoding: chunked
  Date: Thu, 23 Oct 2025 04:55:10 GMT
  Connection: close

  {"error":"Invalid UUID string: {UUID_INVALID}"}
  ```

---

## GET `/users/{userId}/transactions/{transactionId}` — get transaction

### Success
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/transactions/{UUID_VALID}

# nice formatting
http GET http://127.0.0.1:8080/users/{UUID_VALID}/transactions/{UUID_VALID}
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 05:10:00 GMT
  Transfer-Encoding: chunked

  {
    "amount": 20.00,
    "category": "FOOD",
    "date": "2025-10-23",
    "description": "Sandwich",
    "timestamp": "2025-10-23T00:49:52.307021",
    "transactionId": "{UUID_VALID}",
    "userId": "{UUID_VALID}"
  }
  ```

### Failure (invalid or not found)
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_INVALID}/transactions/{UUID_INVALID}
```
- Sample output:
  ```bash
  HTTP/1.1 404
  Content-Type: application/json
  Transfer-Encoding: chunked
  Date: Thu, 23 Oct 2025 05:10:30 GMT

  {"error":"Transaction {UUID_INVALID} for user {UUID_INVALID} not found"}
  ```

---

## POST `/users/{userId}/transactions` (JSON) — create transaction

### Success
```bash
curl -isS -X POST http://127.0.0.1:8080/users/{UUID_VALID}/transactions \
  -H "Content-Type: application/json" \
  -d '{"description":"Sandwich","amount":9.99,"category":"FOOD"}'

# nice formatting
http POST http://127.0.0.1:8080/users/{UUID_VALID}/transactions \
  description="Sandwich" \
  amount:=9.99 \
  category="FOOD"
```
- Sample output:
  ```bash
  HTTP/1.1 201
  Connection: keep-alive
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 04:52:11 GMT
  Keep-Alive: timeout=60
  Transfer-Encoding: chunked

  {
    "amount": 9.99,
    "category": "FOOD",
    "date": null,
    "description": "Sandwich",
    "timestamp": null,
    "transactionId": "{UUID_VALID}",
    "userId": "{UUID_VALID}"
  }
  ```

### Failure (invalid user)
```bash
curl -isS -X POST http://127.0.0.1:8080/users/{UUID_INVALID}/transactions \
  -H "Content-Type: application/json" \
  -d '{"description":"Sandwich","amount":9.99,"category":"FOOD"}'
```
- Sample output:
  ```bash
  HTTP/1.1 400
  Content-Type: application/json
  Transfer-Encoding: chunked
  Date: Thu, 23 Oct 2025 04:52:42 GMT
  Connection: close

  {"error":"Invalid UUID string: {UUID_INVALID}"}
  ```

---

## POST `/users/{userId}/transactions/form` (form) — create transaction via form

### Success
```bash
curl -isS -X POST http://127.0.0.1:8080/users/{UUID_VALID}/transactions/form \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "description=Bus&amount=2.50&category=TRANSPORTATION"

# nice formatting
curl -isS -X POST http://127.0.0.1:8080/users/{UUID_VALID}/transactions/form \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "description=Bus&amount=2.50&category=TRANSPORTATION" | lynx -dump -stdin -nolist
```
- Sample output:
  ```bash
  HTTP/1.1 201
  Content-Type: text/html;charset=UTF-8
  Content-Length: 197
  Date: Thu, 23 Oct 2025 04:58:32 GMT

  Transaction Created Successfully!

  Description: Bus
  Amount: $2.50
  Category: TRANSPORTATION
  ```

### Failure (invalid user)
```bash
curl -isS -X POST http://127.0.0.1:8080/users/{UUID_INVALID}/transactions/form \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "description=Bus&amount=2.50&category=TRANSPORTATION"
```
- Sample output:
  ```bash
  HTTP/1.1 400
  Content-Type: application/json
  Transfer-Encoding: chunked
  Date: Thu, 23 Oct 2025 04:59:04 GMT
  Connection: close

  {"error":"Invalid UUID string: {UUID_INVALID}"}
  ```

---

## PUT `/users/{userId}/transactions/{transactionId}` (JSON) — update transaction

### Success
  ```bash
  curl -isS -X PUT http://127.0.0.1:8080/users/{UUID_VALID}/transactions/{UUID_VALID} \
    -H "Content-Type: application/json" \
    -d '{"amount":7.00}'

  # nice formatting
  http PUT http://127.0.0.1:8080/users/{UUID_VALID}/transactions/{UUID_VALID} \
    amount:=7.00
  ```
- Sample output:
  ```bash
  HTTP/1.1 200
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 05:12:10 GMT
  Transfer-Encoding: chunked

  {
    "amount": 7.00,
    "category": "FOOD",
    "date": "2025-10-23",
    "description": "Sandwich",
    "timestamp": "2025-10-23T00:49:52.307021",
    "transactionId": "{UUID_VALID}",
    "userId": "{UUID_VALID}"
  }
  ```

### Failure (invalid or not found)
```bash
curl -isS -X PUT http://127.0.0.1:8080/users/{UUID_VALID}/transactions/{UUID_INVALID} \
  -H "Content-Type: application/json" \
  -d '{"amount":7.00}'
```
- Sample output:
  ```bash
  HTTP/1.1 404
  Content-Type: application/json
  Transfer-Encoding: chunked
  Date: Thu, 23 Oct 2025 05:12:30 GMT

  {"error":"Transaction {UUID_INVALID} for user {UUID_VALID} not found"}
  ```

---

## GET `/users/{userId}/transactions/create-form` — serve transaction form

### Success
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/transactions/create-form

# nice formatting
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/transactions/create-form | lynx -dump -stdin -nolist
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Content-Type: text/html;charset=UTF-8
  Content-Length: 793
  Date: Thu, 23 Oct 2025 05:00:46 GMT

  Create New Transaction

    Description: ____________________
    Amount: ____________________
    Category: [Food__________]
    Create Transaction
  ```

### Failure (invalid user)
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_INVALID}/transactions/create-form
```
- Sample output:
  ```bash
  HTTP/1.1 400
  Content-Type: application/json
  Transfer-Encoding: chunked
  Date: Thu, 23 Oct 2025 05:01:15 GMT
  Connection: close

  {"error":"Invalid UUID string: {UUID_INVALID}"}
  ```

---

## DELETE `/users/{userId}/transactions/{transactionId}` — delete transaction

### Success
```bash
curl -isS -X DELETE http://127.0.0.1:8080/users/{UUID_VALID}/transactions/{UUID_VALID}

# nice formatting
http DELETE http://127.0.0.1:8080/users/{UUID_VALID}/transactions/{UUID_VALID}
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 05:14:00 GMT
  Transfer-Encoding: chunked

  {"deleted": true, "transactionId": "{UUID_VALID}", "userId": "{UUID_VALID}"}
  ```

### Failure (invalid or not found)
```bash
curl -isS -X DELETE http://127.0.0.1:8080/users/{UUID_VALID}/transactions/{UUID_INVALID}
```
- Sample output:
  ```bash
  HTTP/1.1 404
  Content-Type: application/json
  Transfer-Encoding: chunked
  Date: Thu, 23 Oct 2025 05:14:20 GMT

  {"error":"Transaction {UUID_INVALID} for user {UUID_VALID} not found"}
  ```

---

## GET `/users/{userId}/deletetransaction/{transactionId}` — delete transaction via GET

### Success
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/deletetransaction/{UUID_VALID}

# nice formatting
http GET http://127.0.0.1:8080/users/{UUID_VALID}/deletetransaction/{UUID_VALID}
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Content-Type: text/plain;charset=UTF-8
  Content-Length: 28
  Date: Thu, 23 Oct 2025 05:15:30 GMT

  Transaction deleted successfully
  ```

### Failure (invalid or not found)
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/deletetransaction/{UUID_INVALID}
```
- Sample output:
  ```bash
  HTTP/1.1 404
  Content-Type: application/json
  Transfer-Encoding: chunked
  Date: Thu, 23 Oct 2025 05:15:50 GMT

  {"error":"Transaction {UUID_INVALID} for user {UUID_VALID} not found"}
  ```

---

## GET `/users/{userId}/budget` — budget page

### Success
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/budget

# nice formatting
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/budget | lynx -dump -stdin -nolist
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Content-Type: text/html;charset=UTF-8
  Content-Length: 830
  Date: Thu, 23 Oct 2025 04:33:36 GMT

                            Budget Management - user

  Current Budget

    Total Budget: $200.00
    Total Spent: $0.0
    Remaining: $200.0
    Weekly Spending: $0.00

  Update Budget

    New Budget: 200.0_______________
    Update Budget

  Quick Reports

    * Weekly Summary
    * Monthly Summary
    * Budget Report (JSON)
  ```

### Failure (invalid user)
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_INVALID}/budget
```
- Sample output:
  ```bash
  HTTP/1.1 400
  Content-Type: application/json
  Transfer-Encoding: chunked
  Date: Thu, 23 Oct 2025 04:34:29 GMT
  Connection: close

  {"error":"Invalid UUID string: {UUID_INVALID}"}
  ```

---

## PUT `/users/{userId}/budget` (JSON) — update budget

### Success
```bash
curl -isS -X PUT http://127.0.0.1:8080/users/{UUID_VALID}/budget \
  -H "Content-Type: application/json" \
  -d '{"budget":300}'

# nice formatting
http PUT http://127.0.0.1:8080/users/{UUID_VALID}/budget budget:=300
  ```
- Sample output:
  ```bash
  HTTP/1.1 200
  Connection: keep-alive
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 03:59:18 GMT
  Keep-Alive: timeout=60
  Transfer-Encoding: chunked

  {
    "categories": {},
    "hasWarnings": false,
    "isOverBudget": false,
    "remaining": 300.0,
    "totalBudget": 300.0,
    "totalSpent": 0.0,
    "userId": "{UUID_VALID}",
    "username": "user",
    "warnings": ""
  }
  ```

### Failure (invalid user)
```bash
curl -isS -X PUT http://127.0.0.1:8080/users/{UUID_INVALID}/budget \
  -H "Content-Type: application/json" \
  -d '{"budget":300}'
```
- Sample output:
  ```bash
  HTTP/1.1 400
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 03:59:56 GMT
  Transfer-Encoding: chunked

  {"error":"Invalid UUID string: {UUID_INVALID}"}
  ```

---

## POST `/users/{userId}/update-budget` (form) — update budget via form

### Success
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/update-budget \
  -d "budget=200"

# nice formatting
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/update-budget \
  -d "budget=200" | lynx -dump -stdin -nolist
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Content-Type: text/html;charset=UTF-8
  Content-Length: 106
  Date: Thu, 23 Oct 2025 03:54:58 GMT

  Budget Updated Successfully!

  New Budget: $200.00
  ```

### Failure (invalid user)
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_INVALID}/update-budget \
  -d "budget=200"
```
- Sample output:
  ```bash
  HTTP/1.1 400
  Content-Type: application/json
  Transfer-Encoding: chunked
  Date: Thu, 23 Oct 2025 03:56:06 GMT
  Connection: close

  {"error":"Invalid UUID string: {UUID_INVALID}"}
  ```

---

## GET `/users/{userId}/weekly-summary` — weekly summary page

### Success
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/weekly-summary

# nice formatting
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/weekly-summary | lynx -dump -stdin -nolist
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Content-Type: text/html;charset=UTF-8
  Content-Length: 180
  Date: Thu, 23 Oct 2025 03:48:42 GMT

                  Weekly Summary - user

  Total Spent Last 7 Days: $0.00

  Recent Transactions

    No transactions in the last 7 days.
  ```

### Failure (invalid user)
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_INVALID}/weekly-summary
```
- Sample output:
  ```bash
  HTTP/1.1 400
  Content-Type: application/json
  Transfer-Encoding: chunked
  Date: Thu, 23 Oct 2025 03:49:30 GMT
  Connection: close

  {"error":"Invalid UUID string: {UUID_INVALID}"}
  ```

---

## GET `/users/{userId}/monthly-summary` — monthly summary page

### Success
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/monthly-summary

# nice formatting
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/monthly-summary | lynx -dump -stdin -nolist
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Content-Type: text/html;charset=UTF-8
  Content-Length: 185
  Date: Thu, 23 Oct 2025 03:47:57 GMT

                  Monthly Summary

  Monthly Summary for user (OCTOBER 2025)

  Total Budget: $200.00
  Total Spent: $0.00
  Remaining: $200.00

  Spending by Category:
  ```

### Failure (invalid user)
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_INVALID}/monthly-summary
```
- Sample output:
  ```bash
  HTTP/1.1 400
  Content-Type: application/json
  Transfer-Encoding: chunked
  Date: Thu, 23 Oct 2025 04:26:48 GMT
  Connection: close

  {"error":"Invalid UUID string: {UUID_INVALID}"}
  ```

---

## GET `/users/{userId}/budget-report` — budget report JSON

### Success
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_VALID}/budget-report

# nice formatting
http GET http://127.0.0.1:8080/users/{UUID_VALID}/budget-report
```
- Sample output:
  ```bash
  HTTP/1.1 200
  Connection: keep-alive
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 03:45:40 GMT
  Keep-Alive: timeout=60
  Transfer-Encoding: chunked

  {
    "categories": {},
    "hasWarnings": false,
    "isOverBudget": false,
    "remaining": 200.0,
    "totalBudget": 200.0,
    "totalSpent": 0.0,
    "userId": "{UUID_VALID}",
    "username": "user",
    "warnings": ""
  }
  ```

### Failure (invalid user)
```bash
curl -isS http://127.0.0.1:8080/users/{UUID_INVALID}/budget-report
```
- Sample output:
  ```bash
  HTTP/1.1 400
  Connection: close
  Content-Type: application/json
  Date: Thu, 23 Oct 2025 03:46:30 GMT
  Transfer-Encoding: chunked

  {"error":"Invalid UUID string: {UUID_INVALID}"}
  ```
