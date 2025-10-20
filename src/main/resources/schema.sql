DROP TABLE IF EXISTS transactions CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TYPE IF EXISTS transaction_category CASCADE;

CREATE TYPE transaction_category AS ENUM (
    'FOOD', 'TRANSPORTATION', 'ENTERTAINMENT', 'UTILITIES', 
    'SHOPPING', 'HEALTHCARE', 'TRAVEL', 'EDUCATION', 'OTHER'
);

CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE,
    email VARCHAR(100) UNIQUE,
    budget DECIMAL(10,2)
);

CREATE TABLE transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    description TEXT,
    amount DECIMAL(10,2) NOT NULL,
    category transaction_category NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_date DATE DEFAULT CURRENT_DATE
);