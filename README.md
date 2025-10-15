# ASE-Team-Project
Repository for Team GLACJ's Fall 2025 Advanced Software Engineering team project.

## To Run the Application
```bash
mvn compile
mvn spring-boot:run
```

Once started, navigate to [http://127.0.0.1:8080] in your web browser (or go to Postman but I haven't set that up yet lol)

To developers: Make sure the the sql instance running our database is started while you work and stopped afterwards
- Go to this link:[text](https://console.cloud.google.com/welcome/new?authuser=1&project=ase-group-project-474618)
- Log in/create account with your student email
- Hit three lines at top left, go down to Cloud SQL>instances
- Enter instance "ase-project-db" by hitting its name, then hit start/stop button as needed

## Available Endpoints

## Summary
This ledger service provides:
- User management (`/users`)
- Transaction management (`/update-transaction`)
- Monthly spend summaries (`/monthly-summary`)
- Budget management (`/view-budget` and `/set-budget`)

---

### `/` or `/index`

Displays a welcome message with list of all users

---

#### `/users`
**GET** - Returns list of all users as JSON  
**POST** - Creates new user (JSON)

#### `/users/{userId}`
**GET** - Returns user details as JSON  
**PUT** - Updates user (JSON)  
**DELETE** - Deletes user

#### `/users/form`
**POST** - Creates new user (HTML form)

#### `/deleteuser/{userId}`
**GET** - Deletes user (browser-friendly)

---

### Transaction Management

#### `/users/{userId}/transactions`
**GET** - Returns all transactions for user as JSON  
**POST** - Creates new transaction (JSON)

#### `/users/{userId}/transactions/{transactionId}`
**GET** - Returns specific transaction as JSON  
**PUT** - Updates transaction (JSON)  
**DELETE** - Deletes transaction

#### `/users/{userId}/transactions/form`
**POST** - Creates new transaction (HTML form)

#### `/users/{userId}/deletetransaction/{transactionId}`
**GET** - Deletes transaction (browser-friendly)

---

### Budget & Analytics

#### `/users/{userId}/budget`
**GET** - Budget management page (HTML)  
**PUT** - Updates budget (JSON)

#### `/users/{userId}/budget-report`
**GET** - Returns budget report as JSON

#### `/users/{userId}/monthly-summary`
**GET** - Monthly spending summary (HTML)

#### `/users/{userId}/weekly-summary`
**GET** - Weekly transaction summary (HTML)

#### `/users/{userId}/update-budget`
**POST** - Updates budget (HTML form)

---