-- Insert users (let database generate UUIDs)
INSERT INTO users (username, email, budget) VALUES
('john_doe', 'john.doe@email.com', 1500.00),
('jane_smith', 'jane.smith@email.com', 2500.50),
('bob_wilson', 'bob.wilson@email.com', 800.75);

-- Insert transactions using subqueries to get user UUIDs
INSERT INTO transactions (user_id, description, amount, category)
SELECT user_id, 'Groceries at Whole Foods', 85.75, 'FOOD'
FROM users WHERE username = 'john_doe';

INSERT INTO transactions (user_id, description, amount, category)
SELECT user_id, 'Gas for car', 45.50, 'TRANSPORTATION'
FROM users WHERE username = 'john_doe';

INSERT INTO transactions (user_id, description, amount, category)
SELECT user_id, 'Dinner at Italian restaurant', 67.80, 'FOOD'
FROM users WHERE username = 'jane_smith';