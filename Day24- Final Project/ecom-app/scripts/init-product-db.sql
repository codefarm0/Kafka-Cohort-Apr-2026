-- Product catalog DB (HLD). Used by product-service + Debezium CDC → Kafka → Elasticsearch.

USE product_db;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id VARCHAR(255) NOT NULL UNIQUE,
    sku VARCHAR(255),
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    category_id VARCHAR(64),
    price DECIMAL(10, 2) NOT NULL,
    available_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id),
    INDEX idx_product_name (product_name),
    INDEX idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'debezium'@'%';
FLUSH PRIVILEGES;

INSERT INTO products (product_id, sku, product_name, description, category_id, price, available_quantity, reserved_quantity) VALUES
('product-1', 'SKU-1', 'Sample Product 1', 'Description for Product 1', 'electronics', 29.99, 100, 0),
('product-2', 'SKU-2', 'Sample Product 2', 'Description for Product 2', 'electronics', 49.99, 50, 0),
('product-3', 'SKU-3', 'Sample Product 3', 'Description for Product 3', 'books', 19.99, 200, 0)
ON DUPLICATE KEY UPDATE product_id=product_id;
