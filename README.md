# ASE-Team-Project
Repository for Team GLACJ's Fall 2025 Advanced Software Engineering team project.

## To Run the Application
```bash
mvn compile
mvn spring-boot:run
```

Once started, navigate to [http://127.0.0.1:8080] in your web browser (or go to Postman but I haven't set that up yet lol)

## Available Endpoints

## Summary
This ledger service provides:
- Transaction management (`/update-transaction`)
- Monthly spend summaries (`/monthly-summary`)
- Budget management (`/view-budget` and `/set-budget`)
- Automatic warnings for over-budget categories and totals

---

### `/` or `/index`

Displays a welcome message with current total and category budgets.
- If any budget or category exceeds its limit, a warning message is also displayed.

---

### `/monthly-summary`
Returns a detailed monthly summary of all transactions in the current month, including:
- Total spending
- Category breakdowns (percentage and dollar amounts)
- Budget overview and warnings (if over budget)
---

### `/update-transaction/{id}`
Updates the transaction with the specified `id`.
- **Allowed fields:** `amount`, `category`, `description`
- **Immutable fields:** `id`, `date`

#### Example
```bash
curl -X PATCH "http://localhost:8080/update-transaction/1" \
  -H "Content-Type: application/json" \
  -d '{"amount": 38.75, "category": "Dining", "description": "noodles"}'
```
---

### `/view-budget`
Returns the current total and per-category budgets as JSON, including:
- Budgeted amount
- Spent amount
- Remaining balance
- `overBudget` flag for exceeded categories

#### Example Response
```json
{
  "period": "2025-10",
  "summary": {
    "Total": {
      "budget": 500,
      "spent": 100,
      "remaining": 400,
      "overBudget": false
    },
    "Health": {
      "budget": 50,
      "spent": 0,
      "remaining": 50,
      "overBudget": false
    },
    "Dining": {
      "budget": 100,
      "spent": 75,
      "remaining": 225,
      "overBudget": false
    },
    "Groceries": {
      "budget": 150,
      "spent": 50,
      "remaining": 100,
      "overBudget": false
    },
  }
}
```
---

### `/set-budget`
Updates the budget configuration.

#### Rules
- You can add, update, or delete categories.
- Set a category’s budget to **0** to remove it.
- The sum of all category budgets must equal the **Total**.
- If `Total` is omitted, category updates must still sum to the existing total.
- New categories can be added at any time.

#### Example
```bash
curl -X PATCH "http://localhost:8080/set-budget" \
  -H "Content-Type: application/json" \
  -d '{"Total": 1050, "Dining": 250, "Groceries": 0, "Entertainment": 200, "Education": 450}'
```
