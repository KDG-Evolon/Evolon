-- ========== CLEAN DROP (依存順) ==========
DROP TABLE IF EXISTS chat CASCADE;
DROP TABLE IF EXISTS favorite_item CASCADE;
DROP TABLE IF EXISTS review CASCADE;
DROP TABLE IF EXISTS app_order CASCADE;
DROP TABLE IF EXISTS card_info CASCADE;
DROP TABLE IF EXISTS item CASCADE;
DROP TABLE IF EXISTS category CASCADE;
DROP TABLE IF EXISTS review_stats CASCADE;
DROP TABLE IF EXISTS user_complaint CASCADE;
DROP TABLE IF EXISTS inquiry CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS card_master CASCADE;

-- ========== USERS ==========
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
    reset_token VARCHAR(255),
    reset_token_expires_at TIMESTAMP,

    -- profile
    nickname VARCHAR(50),
    profile_image_url TEXT,
    last_name VARCHAR(50),
    first_name VARCHAR(50),
    postal_code VARCHAR(8),
    address TEXT,
    bio VARCHAR(500)
);


-- ========== CATEGORY ==========
CREATE TABLE category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- ========== ITEM ==========
CREATE TABLE item (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,

    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL,

    category_id INT,

    status VARCHAR(20) NOT NULL DEFAULT 'SELLING',

    image_url TEXT,
	image_url2 TEXT,
  	image_url3 TEXT,
  	image_url4 TEXT,
  	image_url5 TEXT,
	image_url6 TEXT,
	image_url7 TEXT,
	image_url8 TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    shipping_duration VARCHAR(50) NOT NULL,
    shipping_fee_burden VARCHAR(50) NOT NULL,
    shipping_region VARCHAR(50) NOT NULL,
    shipping_method VARCHAR(50) NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES category(id)
);

-- ========== CARD INFO ==========
CREATE TABLE card_info (
    id SERIAL PRIMARY KEY,
    item_id INT NOT NULL UNIQUE,

    card_name VARCHAR(255) NOT NULL,
    pack_name VARCHAR(255),
    rarity VARCHAR(50) NOT NULL,
    regulation VARCHAR(50) NOT NULL,
    condition VARCHAR(50) NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE
);

-- ========== ORDER ==========
CREATE TABLE app_order (
    id SERIAL PRIMARY KEY,
    item_id INT NOT NULL,
    buyer_id INT NOT NULL,
    price NUMERIC(10,2) NOT NULL,

    status VARCHAR(20) DEFAULT '購入済',
    order_status VARCHAR(30) NOT NULL DEFAULT 'PAYMENT_PENDING',

    payment_intent_id VARCHAR(128),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    cancelled_at TIMESTAMP,
    cancelled_by VARCHAR(20),
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,

    FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE,
    FOREIGN KEY (buyer_id) REFERENCES users(id)
);

-- ========== CHAT ==========
CREATE TABLE chat (
    id SERIAL PRIMARY KEY,
    item_id INT NOT NULL,
    sender_id INT NOT NULL,
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,

    FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id)
);

-- ========== FAVORITE ==========
CREATE TABLE favorite_item (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    item_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (user_id, item_id),

    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE
);

-- ========== REVIEW ==========
CREATE TABLE review (
    id SERIAL PRIMARY KEY,

    order_id INT NOT NULL,
    reviewer_id INT NOT NULL,
    reviewee_id INT NOT NULL,
    seller_id INT NOT NULL,
    item_id INT NOT NULL,

    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    result VARCHAR(10) NOT NULL CHECK (result IN ('GOOD','BAD')),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (order_id, reviewer_id),

    FOREIGN KEY (order_id) REFERENCES app_order(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewer_id) REFERENCES users(id),
    FOREIGN KEY (reviewee_id) REFERENCES users(id),
    FOREIGN KEY (seller_id) REFERENCES users(id),
    FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE
);

-- ========== REVIEW STATS ==========
CREATE TABLE review_stats (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    avg_rating NUMERIC(3,2) DEFAULT 0,
    review_count INT DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ========== COMPLAINT ==========
CREATE TABLE user_complaint (
    id SERIAL PRIMARY KEY,
    reported_user_id INT NOT NULL,
    reporter_user_id INT NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (reported_user_id) REFERENCES users(id),
    FOREIGN KEY (reporter_user_id) REFERENCES users(id)
);

-- ========== INQUIRY ==========
CREATE TABLE inquiry (
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

CREATE TABLE  card_master (
    id SERIAL PRIMARY KEY,
    set_code VARCHAR(20) NOT NULL,
    card_number VARCHAR(20) NOT NULL,
    card_name VARCHAR(255) NOT NULL,
    pack_name VARCHAR(255),
    rarity VARCHAR(50) NOT NULL,
    printed_regulation VARCHAR(5) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (set_code, card_number)
);



-- ========== INDEX ==========
CREATE INDEX idx_item_user_id ON item(user_id);
CREATE INDEX idx_item_category_id ON item(category_id);

CREATE INDEX idx_order_item_id ON app_order(item_id);
CREATE INDEX idx_order_buyer_id ON app_order(buyer_id);

CREATE INDEX idx_chat_item_id ON chat(item_id);
CREATE INDEX idx_chat_sender_id ON chat(sender_id);

CREATE INDEX idx_fav_user_id ON favorite_item(user_id);
CREATE INDEX idx_fav_item_id ON favorite_item(item_id);

CREATE INDEX idx_review_order_id ON review(order_id);
CREATE INDEX idx_review_reviewee_id ON review(reviewee_id);
