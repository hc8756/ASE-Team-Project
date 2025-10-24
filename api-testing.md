# API Testing with curl

For setup and run instructions, please see `README.md` in the root of this repository.

You can test every endpoint in this application using `curl`. By default, the server runs at `http://127.0.0.1:8080` or `http://localhost:8080`. These examples use `http://localhost:8080`. Some curl commands also in refer to user/transaction IDs placeholder for real values.

Note: For more visually appealing formatting, we use **Lynx**. If you want this too, install these tools.

Mac:
```
brew install lynx
```
Windows:
```
sudo apt install lynx
```

---

## GET `/` or `/index` — homepage

### Typical Valid Input — Normal case with existing users
```
# Create a few users if needed
curl -isS -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username": "Alice", "email": "alice@columbia.edu", "budget": 500}' ; echo

curl -isS -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username": "Bob", "email": "bob@gmail.com", "budget": 1200}' ; echo

# Access the home page using either / or /index
curl -isS http://localhost:8080/ ; echo
curl -isS http://localhost:8080/index ; echo

# Nice formatting
curl -isS http://localhost:8080/index -o response.html \
&& echo \
&& awk '/^\r?$/{print; exit} {print}' response.html \
&& echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist

```

Sample Output:
```
HTTP/1.1 200 
Content-Type: text/html
Content-Length: 310
Date: Fri, 24 Oct 2025 00:30:03 GMT


                    Welcome to the Personal Finance Tracker

   Existing Users:
   - Alice | alice@columbia.edu | Budget: $500.00
   - Bob | bob@gmail.com | Budget: $1200.00
```

### Atypical Valid Input — No users exist
```
# Delete existing users if needed
# Replace {UUID} with actual IDs from GET /users
curl -isS http://localhost:8080/deleteuser/{UUID} ; echo

# Acces the home page using either / or /index
curl -isS http://localhost:8080/ ; echo
curl -isS http://localhost:8080/index ; echo

# Nice formatting
curl -isS http://localhost:8080/index -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: text/html
Content-Length: 96
Date: Fri, 24 Oct 2025 00:31:11 GMT


                    Welcome to the Personal Finance Tracker

   No users found.
```

### Invalid Input — Wrong path
```
# Misspelled endpoint
curl -isS http://localhost:8080/inde ; echo
```

Sample Output:
```
HTTP/1.1 404 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 00:31:24 GMT

{"timestamp":"2025-10-24T00:31:24.879+00:00","status":404,"error":"Not Found","path":"/inde"}
```

---

## GET `/users` — Retrieve all users

### Typical Valid Input — Normal case with existing users
```
# Create a few users if needed
curl -isS -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username": "Alice", "email": "alice@columbia.edu", "budget": 500}' ; echo

curl -isS -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username": "Bob", "email": "bob@gmail.com", "budget": 1200}' ; echo

# Retrieve all users
curl -isS http://localhost:8080/users -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 00:33:40 GMT


   [{"userId":"31cc9c67-c23c-462b-9a13-a1db18530ee2","username":"Alice","e
   mail":"alice@columbia.edu","budget":500.0},{"userId":"a031ac86-5c6a-473
   2-8742-16b6fb83c11a","username":"Bob","email":"bob@gmail.com","budget":
   1200.0}]
```

### Atypical Valid Input — No users exist
```
# Delete existing users if needed
# Replace {UUID} with actual IDs from GET /users
curl -isS http://localhost:8080/deleteuser/{UUID} ; echo

# Retrieve all users again
curl -isS http://localhost:8080/users -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 00:31:58 GMT


   []
```

### Invalid Input — Wrong path
```
# Misspelled endpoint
curl -isS http://localhost:8080/user ; echo
```

Sample Output:
```
HTTP/1.1 404 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 00:34:20 GMT

{"timestamp":"2025-10-24T00:34:20.235+00:00","status":404,"error":"Not Found","path":"/user"}
```

---

## GET `/users/{userId}` — Retrieve details of a specific user

### Typical Valid Input — Retrieve an existing user by ID
```
# Create a user if needed
curl -isS -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username": "Charlie", "email": "charlie@columbia.edu", "budget": 900}' ; echo

curl -isS http://localhost:8080/users/{UUID} -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 00:37:00 GMT


   {"userId":"31cc9c67-c23c-462b-9a13-a1db18530ee2","username":"Alice","em
   ail":"alice@columbia.edu","budget":500.0}
```

### Atypical Valid Input — UUID format is valid but user does not exist
```
# Use a valid UUID that does not correspond to any existing user
curl -isS http://localhost:8080/users/00000000-0000-0000-0000-000000000000 -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 404 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 00:37:52 GMT


   {"error":"User 00000000-0000-0000-0000-000000000000 not found"}
```

### Invalid Input — Invalid UUID
```
# Invalid UUID format
curl -isS http://localhost:8080/users/not-a-valid-uuid ; echo
```

Sample Output:
```
HTTP/1.1 400 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 00:38:16 GMT
Connection: close

{"error":"Invalid UUID string: not-a-valid-uuid"}
```
---

## POST `/users` — Create a new user via JSON

### Typical Valid Input — Normal case with valid JSON data
```
# Create a new user
curl -isS -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username": "Ethan", "email": "ethan@gmail.com", "budget": 950}' \
  -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 201 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 00:39:04 GMT


   {"userId":"00df6c5a-9321-4744-b653-3a15be39f085","username":"Ethan","em
   ail":"ethan@gmail.com","budget":950.0}
```

### Atypical Valid Input — Missing optional field or empty string
```
# Create a user with minimal data or empty name (edge case)
curl -isS -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username": "", "email": "noname@example.com", "budget": 300}' \
  -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 201 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 00:39:47 GMT


   {"userId":"67a337ac-9034-4b52-a463-6fb3e60572ce","username":"","email":
   "noname@example.com","budget":300.0}
```

### Invalid Input — Incomplete JSON
```
# Invalid payload
curl -isS -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d 'not-valid-json' ; echo
```

Sample Output:
```
HTTP/1.1 400 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 00:40:11 GMT
Connection: close

{"timestamp":"2025-10-24T00:40:11.717+00:00","status":400,"error":"Bad Request","path":"/users"}
```

---

## POST `/users/form` — Create a new user via HTML form submission

### Typical Valid Input — Normal case with proper form data
```
# Submit a new user using form-encoded data
curl -isS -X POST http://localhost:8080/users/form \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=Grace&email=grace@gmail.com&budget=650" \
  -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 201 
Content-Type: text/html;charset=UTF-8
Content-Length: 254
Date: Fri, 24 Oct 2025 00:40:48 GMT


User Created Successfully!

   User ID: 149119d7-dab0-4535-a854-8d2e43f930b5

   Username: Grace

   Email: grace@gmail.com

   Budget: $650.00
```

### Atypical Valid Input — Zero budget value
```
# Submit a user with edge-case budget values
curl -isS -X POST http://localhost:8080/users/form \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=ZeroBudget&email=zerobudget@test.com&budget=0" \
  -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 201 
Content-Type: text/html;charset=UTF-8
Content-Length: 261
Date: Fri, 24 Oct 2025 00:42:04 GMT


User Created Successfully!

   User ID: bae3e580-f710-43aa-b5d1-343531682c77

   Username: ZeroBudget

   Email: zerobudget@test.com

   Budget: $0.00
```

### Invalid Input — Missing required form field(s)
```
# Missing budget field
curl -isS -X POST http://localhost:8080/users/form \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=InvalidUser&email=invalid@test.com" ; echo
```

Sample Output:
```
HTTP/1.1 400 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 00:42:33 GMT
Connection: close

{"timestamp":"2025-10-24T00:42:33.378+00:00","status":400,"error":"Bad Request","path":"/users/form"
```

---

## GET `/users/create-form` — Display HTML form for creating a new user

### Typical Valid Input — Normal request to view the form
```
# Access the user creation form
curl -isS http://localhost:8080/users/create-form -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: text/html;charset=UTF-8
Content-Length: 323
Date: Fri, 24 Oct 2025 00:58:34 GMT


Create New User

   Username: ____________________
   Email: ____________________
   Budget: ____________________
   Create User
```

### Atypical Valid Input — Access form repeatedly
```
# Perform multiple requests (user refreshes the form page)
curl -isS http://localhost:8080/users/create-form -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output (Nice Format):
```
HTTP/1.1 200 
Content-Type: text/html;charset=UTF-8
Content-Length: 323
Date: Fri, 24 Oct 2025 00:59:23 GMT


Create New User

   Username: ____________________
   Email: ____________________
   Budget: ____________________
   Create User
```

### Invalid Input — Wrong path/misspelled route
```
# Misspelled endpoint path
curl -isS http://localhost:8080/users/createform ; echo
```

Sample Output:
```
HTTP/1.1 400 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 00:59:43 GMT
Connection: close

{"error":"Invalid UUID string: createform"}
```

---

## GET `/users/{userId}/edit-form` — Display editable form for an existing user

### Typical Valid Input — Retrieve edit form for an existing user
```
# Replace {UUID} below with an existing user's ID
curl -isS http://localhost:8080/users/{UUID}/edit-form -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output (Nice Format):
```
HTTP/1.1 200 
Content-Type: text/html;charset=UTF-8
Content-Length: 420
Date: Fri, 24 Oct 2025 01:01:03 GMT


Edit User

   Username: Ethan_______________
   Email: ethan@gmail.com_____
   Budget: 950.0_______________
   Update User
```

### Atypical Valid Input — Access the edit form multiple times (repeated requests)
```
# Perform multiple consecutive GET requests to the same edit form
curl -isS http://localhost:8080/users/{UUID}/edit-form -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output (Nice Format):
```
HTTP/1.1 200 
Content-Type: text/html;charset=UTF-8
Content-Length: 420
Date: Fri, 24 Oct 2025 01:01:03 GMT


Edit User

   Username: Ethan_______________
   Email: ethan@gmail.com_____
   Budget: 950.0_______________
   Update User
```

### Invalid Input — Nonexistent user ID
```
# Use a UUID that does not exist
curl -isS http://localhost:8080/users/00000000-0000-0000-0000-000000000000/edit-form ; echo
```

Sample Output:
```
HTTP/1.1 404 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:02:01 GMT

{"error":"User 00000000-0000-0000-0000-000000000000 not found"}
```

---

## DELETE `/users/{userId}` — Delete a user by ID (JSON API)

### Typical Valid Input — Delete an existing user
```
# Replace {UUID} with an existing user ID
curl -isS -X DELETE http://localhost:8080/users/{UUID} -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:03:22 GMT


   {"deleted":true,"userId":"67a337ac-9034-4b52-a463-6fb3e60572ce"}
```

### Atypical Valid Input — Delete an already deleted user
```
# Attempt to delete the same user again
curl -isS -X DELETE http://localhost:8080/users/{UUID} -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 404 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:03:47 GMT


   {"error":"User 67a337ac-9034-4b52-a463-6fb3e60572ce not found"}
```

### Invalid Input — Invalid or incorrect UUID
```
# Use an invalid UUID string
curl -isS -X DELETE http://localhost:8080/users/not-a-valid-uuid ; echo
```

Sample Output:
```
HTTP/1.1 400 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:05:23 GMT
Connection: close

{"error":"Invalid UUID string: not-a-valid-uuid"}
```

---

## GET `/deleteuser/{userId}` — Delete a user via GET (HTML Response)

### Typical Valid Input — Delete an existing user
```
# Replace {UUID} with the an existin user ID
curl -isS http://localhost:8080/deleteuser/{UUID} -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: text/plain;charset=UTF-8
Content-Length: 25
Date: Fri, 24 Oct 2025 01:06:35 GMT


   User deleted successfully
```

### Atypical Valid Input — Attempt to delete an already deleted user
```
# Try deleting the same user again
curl -isS http://localhost:8080/deleteuser/{UUID} -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 404 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:07:03 GMT


   {"error":"User bae3e580-f710-43aa-b5d1-343531682c77 not found"}
```

### Invalid Input — Invalid UUID
```
# Step 1: Provide an invalid UUID format
curl -isS http://localhost:8080/deleteuser/not-a-valid-uuid ; echo
```

Sample Output:
```
HTTP/1.1 400 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:07:37 GMT
Connection: close

{"error":"Invalid UUID string: not-a-valid-uuid"}
```

---

## GET `/users/{userId}/transactions` — Retrieve all transactions for a specific user

### Typical Valid Input — User exists and has transactions
```
# Replace {USER_UUID} with the user ID
# Add two transactions for this user
curl -isS -X POST http://localhost:8080/users/{USER_UUID}/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "Groceries", "amount": 120.5, "category": "FOOD"}' ; echo

curl -isS -X POST http://localhost:8080/users/{USER_UUID}/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "MetroCard Refill", "amount": 40, "category": "TRANSPORTATION"}' ; echo

# Retrieve all transactions for the user
curl -isS http://localhost:8080/users/{USER_UUID}/transactions -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:09:54 GMT


   [{"transactionId":"45ed2ed8-2964-4b56-84cb-2d5832d806c7","userId":"00df
   6c5a-9321-4744-b653-3a15be39f085","description":"MetroCard
   Refill","amount":40.0,"category":"TRANSPORTATION","timestamp":"2025-10-
   23T21:09:34.305769","date":"2025-10-23"},{"transactionId":"c7bb37bf-ab0
   4-4e8b-a0ac-d348464a9a65","userId":"00df6c5a-9321-4744-b653-3a15be39f08
   5","description":"Groceries","amount":120.5,"category":"FOOD","timestam
   p":"2025-10-23T21:09:09.821857","date":"2025-10-23"}]
```

### Atypical Valid Input — User exists but has no transactions
```
# Step 1: Create a new user without adding transactions
curl -isS -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username": "Olivia", "email": "olivia@columbia.edu", "budget": 800}' ; echo

# Step 2: Replace {USER_UUID} with the ID from the response

# Step 3: Retrieve transactions (expect an empty array)
curl -isS http://localhost:8080/users/{USER_UUID}/transactions -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:10:33 GMT


   []
```

### Invalid Input — Non-existent or invalid user ID
```
# Non-existent UUID
curl -isS http://localhost:8080/users/00000000-0000-0000-0000-000000000000/transactions ; echo
```

Sample Output:
```
HTTP/1.1 404 
Content-Type: text/plain;charset=UTF-8
Content-Length: 58
Date: Fri, 24 Oct 2025 01:11:14 GMT

Error: User 00000000-0000-0000-0000-000000000000 not found
```

---

## GET `/users/{userId}/transactions/{transactionId}` — Retrieve details of a specific transaction

### Typical Valid Input — Valid user and existing transaction
```
# Replace {USER_UUID} with the valid user ID
# Replace {TRANSACTION_UUID} with existing transaction ID
curl -isS http://localhost:8080/users/{USER_UUID}/transactions/{TRANSACTION_UUID} -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:13:19 GMT


   {"transactionId":"45ed2ed8-2964-4b56-84cb-2d5832d806c7","userId":"00df6
   c5a-9321-4744-b653-3a15be39f085","description":"MetroCard
   Refill","amount":40.0,"category":"TRANSPORTATION","timestamp":"2025-10-
   23T21:09:34.305769","date":"2025-10-23"}
```

### Atypical Valid Input — Valid user but transaction belongs to another user
```
# Create a user if needed
curl -isS -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username": "Isabella", "email": "isabella@columbia.edu", "budget": 950}' ; echo

# Create a transaction for the new user
curl -isS -X POST http://localhost:8080/users/{NEW_USER_UUID}/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "Dinner", "amount": 80.75, "category": "FOOD"}' ; echo

# Try fetching that transaction under a different valid user
curl -isS http://localhost:8080/users/{USER_1_UUID}/transactions/{TRANSACTION_NEW_UUID} -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 404 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:18:23 GMT


   {"error":"Transaction 6ce98f89-de4c-4d44-9d3d-d01024bfbf91 not found
   for user 00df6c5a-9321-4744-b653-3a15be39f085"}
```

### Invalid Input — Nonexistent/invalid UUIDs
```
curl -isS http://localhost:8080/users/{VALID_USER_UUID}/transactions/00000000-0000-0000-0000-000000000000 ; echo
```

Sample Output:
```
HTTP/1.1 400 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:19:00 GMT
Connection: close

{"error":"Invalid UUID string: VALID_USER_UUID"}
```

---

## POST `/users/{userId}/transactions` — Create a new transaction for a user (JSON)

### Typical Valid Input — Valid user with correct JSON payload
```
# Create a transaction for a valid user
curl -isS -X POST http://localhost:8080/users/{USER_UUID}/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "Coffee and Bagel", "amount": 8.75, "category": "FOOD"}' -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 201 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:20:40 GMT


   {"transactionId":"01eb0e59-258e-4828-8ed9-388f534ec498","userId":"00df6
   c5a-9321-4744-b653-3a15be39f085","description":"Coffee and
   Bagel","amount":8.75,"category":"FOOD","timestamp":"2025-10-23T21:20:40
   .313522","date":"2025-10-23"}0
```

### Atypical Valid Input — Zero transaction amount
```
# Create a transaction with a zero amount
curl -isS -X POST http://localhost:8080/users/{USER_UUID}/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "Free coupon coffee", "amount": 0.0, "category": "FOOD"}' -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 201 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:21:30 GMT


   {"transactionId":"2f67194a-442a-4a00-8771-bd7e334c5fe8","userId":"00df6
   c5a-9321-4744-b653-3a15be39f085","description":"Free coupon
   coffee","amount":0.0,"category":"FOOD","timestamp":"2025-10-23T21:21:30
   .343616","date":"2025-10-23"}
```

### Invalid Input — Non-existent user or malformed payload
```
# Non-existent user
curl -isS -X POST http://localhost:8080/users/00000000-0000-0000-0000-000000000000/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "Lunch", "amount": 12.0, "category": "FOOD"}' ; echo

# Malformed JSON payload
curl -isS -X POST http://localhost:8080/users/{USER_UUID}/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "Invalid JSON", "amount": , "category": "FOOD"}' ; echo
```

Sample Output:
```
HTTP/1.1 400 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:22:11 GMT
Connection: close

{"error":"Invalid UUID string: USER_UUID"}
```

---

## POST `/users/{userId}/transactions/form` — Create a new transaction (HTML form)

### Typical Valid Input — Valid user submitting correct form data
```
# Create a new transaction via HTML form submission
curl -isS -X POST http://localhost:8080/users/{USER_UUID}/transactions/form \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "description=Gym+Membership&amount=50.00&category=HEALTHCARE" -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 201 
Content-Type: text/html;charset=UTF-8
Content-Length: 205
Date: Fri, 24 Oct 2025 01:23:05 GMT


Transaction Created Successfully!

   Description: Gym Membership

   Amount: $50.00

   Category: HEALTHCARE
```

### Atypical Valid Input — Edge case with zero amount
```
# Create a transaction with a zero amount
curl -isS -X POST http://localhost:8080/users/{USER_UUID}/transactions/form \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "description=Free+Trial&amount=0&category=OTHER" -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 201 
Content-Type: text/html;charset=UTF-8
Content-Length: 195
Date: Fri, 24 Oct 2025 01:24:15 GMT


Transaction Created Successfully!

   Description: Free Trial

   Amount: $0.00

   Category: OTHER
```

### Invalid Input — Non-existent user or missing parameters
```
# Non-existent user ID
curl -isS -X POST http://localhost:8080/users/00000000-0000-0000-0000-000000000000/transactions/form \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "description=Snacks&amount=5.50&category=FOOD" ; echo

# Missing parameter (no category)
curl -isS -X POST http://localhost:8080/users/{USER_UUID}/transactions/form \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "description=Snacks&amount=5.50" ; echo
```

Sample Output:
```
HTTP/1.1 404 
Content-Type: text/html;charset=UTF-8
Content-Length: 58
Date: Fri, 24 Oct 2025 01:24:47 GMT

Error: User 00000000-0000-0000-0000-000000000000 not found
```

---

## GET `/users/{userId}/transactions/create-form` — Display the HTML form for creating a new transaction

### Typical Valid Input — Display form for an existing user
```
curl -isS http://localhost:8080/users/{USER_UUID}/transactions/create-form -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output (Nice Format):
```
HTTP/1.1 200 
Content-Type: text/html;charset=UTF-8
Content-Length: 793
Date: Fri, 24 Oct 2025 01:49:41 GMT


Create New Transaction

   Description: ____________________
   Amount: ____________________
   Category: [Food__________]
   Create Transaction
```

### Atypical Valid Input — User exists but has no prior transactions
```
# Access the form for a user who has no transactions yet
curl -isS http://localhost:8080/users/{USER_UUID}/transactions/create-form -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output (Nice Format):
```
HTTP/1.1 200 
Content-Type: text/html;charset=UTF-8
Content-Length: 793
Date: Fri, 24 Oct 2025 01:52:00 GMT


Create New Transaction

   Description: ____________________
   Amount: ____________________
   Category: [Food__________]
   Create Transaction
```

### Invalid Input — Nonexistent user ID
```
# Case 1: Non-existent user UUID
curl -isS http://localhost:8080/users/00000000-0000-0000-0000-000000000000/transactions/create-form ; echo
```

Sample Output:
```
HTTP/1.1 404 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:52:49 GMT

{"error":"User 00000000-0000-0000-0000-000000000000 not found"}
```

---

## DELETE `/users/{userId}/transactions/{transactionId}` — Delete a transaction (JSON)

### Typical Valid Input — Delete an existing transaction for a valid user
```
curl -isS -X DELETE http://localhost:8080/users/{USER_UUID}/transactions/{TRANSACTION_UUID} -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:54:27 GMT


   {"deleted":true,"transactionId":"62e35ca8-dc11-403d-ad2d-4e7d538e046c",
   "userId":"00df6c5a-9321-4744-b653-3a15be39f085"}
```

### Atypical Valid Input — Try deleting the same transaction twice
```
# Attempt to delete the same transaction again
curl -isS -X DELETE http://localhost:8080/users/{USER_UUID}/transactions/{TRANSACTION_UUID} -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 404 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:54:39 GMT


   {"error":"Transaction 62e35ca8-dc11-403d-ad2d-4e7d538e046c not found
   for user 00df6c5a-9321-4744-b653-3a15be39f085"}
```

### Invalid Input — Transaction belongs to another user
```
# Transaction belongs to another user
curl -isS -X DELETE http://localhost:8080/users/{DIFFERENT_USER_UUID}/transactions/{TRANSACTION_UUID} ; echo
```

Sample Output:
```
HTTP/1.1 404 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 01:55:32 GMT

{"error":"Transaction 135dd178-9e17-4f30-a6f2-84b6d035fb31 not found for user 149119d7-dab0-4535-a854-8d2e43f930b5"}
```

---

## GET `/users/{userId}/deletetransaction/{transactionId}` — Delete a transaction via GET (plain text)

### Typical Valid Input — Delete an existing transaction
```
curl -isS http://localhost:8080/users/{USER_UUID}/deletetransaction/{TRANSACTION_UUID} -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output (Nice Format):
```
HTTP/1.1 200 
Content-Type: text/plain;charset=UTF-8
Content-Length: 33
Date: Fri, 24 Oct 2025 01:57:01 GMT


   Transaction deleted successfully!
```

### Atypical Valid Input — Attempt to delete the same transaction twice
```
# Attempt to delete again using the same UUIDs
curl -isS http://localhost:8080/users/{USER_UUID}/deletetransaction/{TRANSACTION_UUID} -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output (Nice Format):
```
HTTP/1.1 200 
Content-Type: text/plain;charset=UTF-8
Content-Length: 111
Date: Fri, 24 Oct 2025 01:57:10 GMT


   Error: Transaction 2f67194a-442a-4a00-8771-bd7e334c5fe8 not found for
   user 00df6c5a-9321-4744-b653-3a15be39f085
```

### Invalid Input — Nonexistent user/transaction ID
```
# Non-existent user UUID
curl -isS http://localhost:8080/users/00000000-0000-0000-0000-000000000000/deletetransaction/{TRANSACTION_UUID} ; echo

```

Sample Output:
```
HTTP/1.1 200 
Content-Type: text/plain;charset=UTF-8
Content-Length: 58
Date: Fri, 24 Oct 2025 01:57:54 GMT

Error: User 00000000-0000-0000-0000-000000000000 not found
```

---

## GET `/users/{userId}/budget` — Display the budget management page for a user

### Typical Valid Input — Display budget dashboard for an existing user
```
# Step 1: Create a user
curl -isS -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username": "Sophia", "email": "sophia@columbia.edu", "budget": 1000}' ; echo

# Step 2: Replace {USER_UUID} with the user ID from the response

# Step 3: Add a few transactions for realism
curl -isS -X POST http://localhost:8080/users/{USER_UUID}/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "Lunch", "amount": 25.75, "category": "FOOD"}' ; echo

curl -isS -X POST http://localhost:8080/users/{USER_UUID}/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "MetroCard Refill", "amount": 33.00, "category": "TRANSPORTATION"}' ; echo

# Step 4: View the budget management dashboard
curl -isS http://localhost:8080/users/{USER_UUID}/budget -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: text/html;charset=UTF-8
Content-Length: 835
Date: Fri, 24 Oct 2025 01:58:35 GMT


                           Budget Management - Ethan

Current Budget

   Total Budget: $950.00

   Total Spent: $219.25

   Remaining: $730.75

   Weekly Spending: $0.00

Update Budget

   New Budget: 950.0_______________
   Update Budget

Quick Reports

     * Weekly Summary
     * Monthly Summary
     * Budget Report (JSON)
```

### Atypical Valid Input — User exists but has no transactions
```
# Create a new user with no transactions if needed
curl -isS -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username": "Liam", "email": "liam@columbia.edu", "budget": 500}' ; echo

# Access the budget dashboard
curl -isS http://localhost:8080/users/{USER_UUID}/budget -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: text/html;charset=UTF-8
Content-Length: 830
Date: Fri, 24 Oct 2025 02:00:00 GMT


                            Budget Management - Liam

Current Budget

   Total Budget: $500.00

   Total Spent: $0.0

   Remaining: $500.0

   Weekly Spending: $0.00

Update Budget

   New Budget: 500.0_______________
   Update Budget

Quick Reports

     * Weekly Summary
     * Monthly Summary
     * Budget Report (JSON)
```

### Invalid Input — Nonexistent user ID
```
# Non-existent user
curl -isS http://localhost:8080/users/00000000-0000-0000-0000-000000000000/budget ; echo
```

Sample Output:
```
HTTP/1.1 404 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 02:00:23 GMT

{"error":"User 00000000-0000-0000-0000-000000000000 not found"}
```

---

## PUT `/users/{userId}/budget` — Update a user's budget (JSON)

### Typical Valid Input — Update budget for an existing user
```
curl -isS -X PUT http://localhost:8080/users/{USER_UUID}/budget \
  -H "Content-Type: application/json" \
  -d '{"budget": 1200}' -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 02:01:13 GMT


   {"userId":"00df6c5a-9321-4744-b653-3a15be39f085","warnings":"","remaini
   ng":980.75,"isOverBudget":false,"totalSpent":219.25,"hasWarnings":false
   ,"categories":{"HEALTHCARE":50.0,"TRANSPORTATION":40.0,"FOOD":129.25},"
   totalBudget":1200.0,"username":"Ethan"}
```

### Atypical Valid Input — Budget updated with same value (no effective change)
```
curl -isS -X PUT http://localhost:8080/users/{USER_UUID}/budget \
  -H "Content-Type: application/json" \
  -d '{"budget": 1200}' -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 02:02:15 GMT


   {"userId":"00df6c5a-9321-4744-b653-3a15be39f085","warnings":"","remaini
   ng":980.75,"isOverBudget":false,"totalSpent":219.25,"hasWarnings":false
   ,"categories":{"HEALTHCARE":50.0,"TRANSPORTATION":40.0,"FOOD":129.25},"
   totalBudget":1200.0,"username":"Ethan"}
```

### Invalid Input — Nonexistent user
```
# Non-existent user
curl -isS -X PUT http://localhost:8080/users/00000000-0000-0000-0000-000000000000/budget \
  -H "Content-Type: application/json" \
  -d '{"budget": 1500}' ; echo
```

Sample Output:
```
HTTP/1.1 404 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 02:02:45 GMT

{"error":"User 00000000-0000-0000-0000-000000000000 not found"}
```

---

## POST `/users/{userId}/update-budget` — Update a user’s budget via HTML form

### Typical Valid Input — Update budget for existing user
```
curl -isS -X POST http://localhost:8080/users/{USER_UUID}/update-budget \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "budget=950" -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: text/html;charset=UTF-8
Content-Length: 106
Date: Fri, 24 Oct 2025 02:03:47 GMT


Budget Updated Successfully!

   New Budget: $950.00
```

### Atypical Valid Input — Submit same budget amount (no change)
```
# Submit the same budget value again
curl -isS -X POST http://localhost:8080/users/{USER_UUID}/update-budget \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "budget=950" -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: text/html;charset=UTF-8
Content-Length: 106
Date: Fri, 24 Oct 2025 02:04:03 GMT


Budget Updated Successfully!

   New Budget: $950.00
```

### Invalid Input — Nonexistent user
```
curl -isS -X POST http://localhost:8080/users/00000000-0000-0000-0000-000000000000/update-budget \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "budget=1200" ; echo
```

Sample Output:
```
HTTP/1.1 404 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 02:04:26 GMT

{"error":"User 00000000-0000-0000-0000-000000000000 not found"}
```

---

## GET `/users/{userId}/weekly-summary` — Display the user’s weekly spending summary

### Typical Valid Input — User with transactions in the past week
```
curl -isS http://localhost:8080/users/{USER_UUID}/weekly-summary -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: text/html;charset=UTF-8
Content-Length: 693
Date: Fri, 24 Oct 2025 02:05:22 GMT


                             Weekly Summary - Ethan

   Total Spent Last 7 Days: $0.00

Recent Transactions

     Description    Amount     Category       Date
   Free Trial       $0.00   OTHER          2025-10-23
   Gym Membership   $50.00  HEALTHCARE     2025-10-23
   Coffee and Bagel $8.75   FOOD           2025-10-23
   MetroCard Refill $40.00  TRANSPORTATION 2025-10-23
   Groceries        $120.50 FOOD           2025-10-23
```

### Atypical Valid Input — User exists but has no transactions in the past week
```
curl -isS http://localhost:8080/users/{USER_UUID}/weekly-summary -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: text/html;charset=UTF-8
Content-Length: 180
Date: Fri, 24 Oct 2025 02:05:59 GMT


                             Weekly Summary - Liam

   Total Spent Last 7 Days: $0.00

Recent Transactions

   No transactions in the last 7 days.
```

### Invalid Input — Nonexistent user ID
```
curl -isS http://localhost:8080/users/00000000-0000-0000-0000-000000000000/weekly-summary ; echo
```

Sample Output:
```
HTTP/1.1 404 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 02:06:31 GMT

{"error":"User 00000000-0000-0000-0000-000000000000 not found"}
```

---

## GET `/users/{userId}/monthly-summary` — Display the user’s monthly spending summary

### Typical Valid Input — User with transactions this month
```
curl -isS http://localhost:8080/users/{USER_UUID}/monthly-summary -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: text/html;charset=UTF-8
Content-Length: 251
Date: Fri, 24 Oct 2025 02:07:15 GMT


                                Monthly Summary

Monthly Summary for Ethan (OCTOBER 2025)

Total Budget: $1200.00
Total Spent: $219.25
Remaining: $980.75

Spending by Category:
- FOOD: $129.25
- HEALTHCARE: $50.00
- TRANSPORTATION: $40.00
```

### Atypical Valid Input — User exists but has no transactions this month
```
curl -isS http://localhost:8080/users/{USER_UUID}/monthly-summary -o response.html ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.html ; echo \
&& awk 'f; /^\r?$/{f=1}' response.html | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: text/html;charset=UTF-8
Content-Length: 185
Date: Fri, 24 Oct 2025 02:07:53 GMT


                                Monthly Summary

Monthly Summary for Liam (OCTOBER 2025)

Total Budget: $950.00
Total Spent: $0.00
Remaining: $950.00

Spending by Category:
```

### Invalid Input — Nonexistent user ID
```
curl -isS http://localhost:8080/users/00000000-0000-0000-0000-000000000000/monthly-summary ; echo
```

Sample Output:
```
HTTP/1.1 404 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 02:08:19 GMT

{"error":"User 00000000-0000-0000-0000-000000000000 not found"}
```

---

## GET `/users/{userId}/budget-report` — Retrieve a user’s budget report in JSON

### Typical Valid Input — Retrieve JSON report for existing user
```
curl -isS http://localhost:8080/users/{USER_UUID}/budget-report -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 02:09:10 GMT


   {"userId":"00df6c5a-9321-4744-b653-3a15be39f085","warnings":"","remaini
   ng":980.75,"isOverBudget":false,"totalSpent":219.25,"hasWarnings":false
   ,"categories":{"HEALTHCARE":50.0,"TRANSPORTATION":40.0,"FOOD":129.25},"
   totalBudget":1200.0,"username":"Ethan"}
```

### Atypical Valid Input — No transactions yet
```
curl -isS http://localhost:8080/users/{USER_UUID}/budget-report -o response.json ; echo \
&& awk '/^\r?$/{print; exit} {print}' response.json ; echo \
&& awk 'f; /^\r?$/{f=1}' response.json | lynx -dump -stdin -nolist ; echo
```

Sample Output:
```
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 02:09:47 GMT


   {"userId":"5a3c181f-39ee-42e0-a1ff-88cfc5a301d1","warnings":"","remaini
   ng":950.0,"isOverBudget":false,"totalSpent":0.0,"hasWarnings":false,"ca
   tegories":{},"totalBudget":950.0,"username":"Liam"}
```

### Invalid Input — Nonexistent user ID
```
curl -isS http://localhost:8080/users/00000000-0000-0000-0000-000000000000/budget-report ; echo
```

Sample Output:
```
HTTP/1.1 404 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 24 Oct 2025 02:10:19 GMT

{"error":"User 00000000-0000-0000-0000-000000000000 not found"}
```