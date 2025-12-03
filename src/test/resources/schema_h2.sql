-- Users table
CREATE TABLE users (
    user_id VARCHAR(36) PRIMARY KEY DEFAULT RANDOM_UUID(),
    username VARCHAR(50) UNIQUE,
    email VARCHAR(100) UNIQUE,
    budget DECIMAL(10,2)
);

-- Transactions table
CREATE TABLE transactions (
    transaction_id VARCHAR(36) PRIMARY KEY DEFAULT RANDOM_UUID(),
    user_id VARCHAR(36) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    description TEXT,
    amount DECIMAL(10,2) NOT NULL,
    category VARCHAR(50) NOT NULL, -- store enum values as strings
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_date DATE DEFAULT CURRENT_DATE
);
