-- Create orders table
CREATE TABLE IF NOT EXISTS orders (
    order_id SERIAL PRIMARY KEY,
    customer_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    order_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create index on created_at for timestamp mode
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_orders_updated_at ON orders(updated_at);

-- Insert sample data
INSERT INTO orders (customer_id, product_id, quantity, price, order_status) VALUES
(1001, 2001, 2, 59.98, 'PENDING'),
(1002, 2002, 1, 89.99, 'CONFIRMED'),
(1003, 2003, 3, 137.97, 'SHIPPED'),
(1004, 2001, 1, 29.99, 'DELIVERED'),
(1005, 2004, 2, 599.98, 'PENDING');

-- Create data warehouse table (for sink connector)
CREATE TABLE IF NOT EXISTS orders_warehouse (
    order_id INTEGER PRIMARY KEY,
    customer_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    order_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    kafka_topic VARCHAR(255),
    kafka_partition INTEGER,
    kafka_offset BIGINT,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

