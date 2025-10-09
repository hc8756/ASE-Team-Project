CREATE TYPE IF NOT EXISTS transaction_category AS ENUM (
    'FOOD', 'TRANSPORTATION', 'ENTERTAINMENT', 'UTILITIES', 
    'SHOPPING', 'HEALTHCARE', 'TRAVEL', 'EDUCATION', 'OTHER'
);

CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE,
    email VARCHAR(100) UNIQUE,
    budget DECIMAL(10,2)
);

CREATE TABLE IF NOT EXISTS transactions (
    trans_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    description TEXT,
    amount DECIMAL(10,2) NOT NULL,
    category transaction_category NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);