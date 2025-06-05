DROP TABLE IF EXISTS order_product;
DROP TABLE IF EXISTS customer_order;
DROP TABLE IF EXISTS product;

-- Create Product table
CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    price DECIMAL(10, 2),
    img_path VARCHAR(500),
    count INTEGER
);

-- Create CustomerOrder table
CREATE TABLE customer_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    total_sum DECIMAL(10, 2)
);

-- Create OrderProduct table (junction table)
CREATE TABLE order_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER,
    FOREIGN KEY (order_id) REFERENCES customer_order(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);