-- ========== CLEAN DROP (依存順) ==========
DROP TABLE IF EXISTS chat CASCADE;
DROP TABLE IF EXISTS favorite_item CASCADE;
DROP TABLE IF EXISTS review CASCADE;
DROP TABLE IF EXISTS app_order CASCADE;
DROP TABLE IF EXISTS item CASCADE;
DROP TABLE IF EXISTS category CASCADE;
DROP TABLE IF EXISTS review_stats CASCADE;
DROP TABLE IF EXISTS card_info CASCADE;
DROP TABLE IF EXISTS user_complaint CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ========== CREATE ==========
-- ユーザー情報
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    line_notify_token VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    banned BOOLEAN NOT NULL DEFAULT FALSE,
    ban_reason TEXT,
    banned_at TIMESTAMP,
    banned_by_admin_id INT,
    --パスワード再設定用
    reset_token VARCHAR(255),
    reset_token_expires_at TIMESTAMP
);

-- カテゴリ
CREATE TABLE category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- 商品
CREATE TABLE item (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL,
    category_id INT,
    status VARCHAR(20) DEFAULT '出品中',
    listing_type VARCHAR(50),
    image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    postage NUMERIC(10,2),
    local VARCHAR(255),
    condition VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES category(id)
);

-- 注文
CREATE TABLE app_order (
    id SERIAL PRIMARY KEY,
    item_id INT NOT NULL,
    buyer_id INT NOT NULL,
    price NUMERIC(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT '購入済',
    payment_intent_id VARCHAR(128),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES item(id),
    FOREIGN KEY (buyer_id) REFERENCES users(id)
);

-- チャット
CREATE TABLE chat (
    id SERIAL PRIMARY KEY,
    item_id INT NOT NULL,
    sender_id INT NOT NULL,
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES item(id),
    FOREIGN KEY (sender_id) REFERENCES users(id)
);

-- お気に入り
CREATE TABLE favorite_item (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    item_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, item_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (item_id) REFERENCES item(id)
);

-- レビュー
CREATE TABLE review (
    id SERIAL PRIMARY KEY,
    order_id INT NOT NULL UNIQUE,
    reviewer_id INT NOT NULL,
    seller_id INT NOT NULL,
    item_id INT NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES app_order(id),
    FOREIGN KEY (reviewer_id) REFERENCES users(id),
    FOREIGN KEY (seller_id) REFERENCES users(id),
    FOREIGN KEY (item_id) REFERENCES item(id)
);

-- レビュー統計
CREATE TABLE review_stats (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    avg_rating NUMERIC(3,2) DEFAULT 0,
    review_count INT DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- カード情報
CREATE TABLE card_info (
    id SERIAL PRIMARY KEY,
    item_id INT NOT NULL,
    pack VARCHAR(255),
    rarity VARCHAR(50),
    regulation VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES item(id)
);

-- 通報
CREATE TABLE user_complaint (
    id SERIAL PRIMARY KEY,
    reported_user_id INT NOT NULL,
    reporter_user_id INT NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reported_user_id) REFERENCES users(id),
    FOREIGN KEY (reporter_user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS inquiry (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'OPEN',
    admin_reply TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    replied_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);




-- ========== INDEX ==========
CREATE INDEX idx_users_banned ON users(banned);
CREATE INDEX idx_users_banned_by ON users(banned_by_admin_id);
CREATE INDEX idx_item_user_id ON item(user_id);
CREATE INDEX idx_item_category_id ON item(category_id);
CREATE INDEX idx_order_item_id ON app_order(item_id);
CREATE INDEX idx_order_buyer_id ON app_order(buyer_id);
CREATE UNIQUE INDEX ux_order_pi ON app_order(payment_intent_id);
CREATE INDEX idx_chat_item_id ON chat(item_id);
CREATE INDEX idx_chat_sender_id ON chat(sender_id);
CREATE INDEX idx_fav_user_id ON favorite_item(user_id);
CREATE INDEX idx_fav_item_id ON favorite_item(item_id);
CREATE INDEX idx_review_order_id ON review(order_id);
CREATE INDEX idx_uc_reported ON user_complaint(reported_user_id);
CREATE INDEX idx_uc_reporter ON user_complaint(reporter_user_id);

