-- ========================================================
-- Secure UPI Network Simulator - MySQL Database Schema
-- ========================================================

CREATE DATABASE IF NOT EXISTS secure_upi_db;
USE secure_upi_db;

-- 1. Users Table (Stores user credentials, bank account numbers, phone, and balances)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    payment_password VARCHAR(255) NOT NULL,
    account_number VARCHAR(12) UNIQUE NOT NULL,
    phone VARCHAR(10) UNIQUE NOT NULL,
    balance DECIMAL(12, 2) NOT NULL DEFAULT 5000.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Global Transactions Table (Global audit ledger of all UPI payments)
CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(50) UNIQUE NOT NULL,
    sender_account VARCHAR(12) NOT NULL,
    receiver_account VARCHAR(12) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sender (sender_account),
    INDEX idx_receiver (receiver_account)
);

-- Seed Default / Demo Users so existing accounts remain fully functional
INSERT IGNORE INTO users (username, password, payment_password, account_number, phone, balance) VALUES
('Sarvesh1', 'Computer1@', 'Sarvesh@123', '111100000001', '9000000001', 5000.00),
('Srimano2', 'Computer1@', 'Srimano@123', '111100000002', '9000000002', 3500.00),
('Kasi3',    'Computer1@', 'Kasi@123',    '111100000003', '9000000003', 4200.00),
('Hasiba4',  'Computer1@', 'Hasiba@123',  '111100000004', '9000000004', 6000.00);

-- Initial seed sample transactions for global history demonstration
INSERT IGNORE INTO transactions (transaction_id, sender_account, receiver_account, amount, status) VALUES
('TXN-SAMPLE000001', '111100000001', '111100000002', 500.00, 'SUCCESS'),
('TXN-SAMPLE000002', '111100000003', '111100000004', 1200.00, 'SUCCESS');
