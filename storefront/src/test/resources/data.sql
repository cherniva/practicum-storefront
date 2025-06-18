INSERT INTO product (name, description, price, img_path) VALUES
('Test Product 1', 'Description 1', 99.99, '/images/product1.jpg'),
('Test Product 2', 'Description 2', 149.99, '/images/product2.jpg'),
('Test Product 3', 'Description 3', 199.99, '/images/product3.jpg');

INSERT INTO user (id, username, password) values
(1, 'vokinrehc', '1234');

INSERT INTO customer_order (user_id, total_sum) VALUES
(1, 249.98),
(1, 199.99);

INSERT INTO order_product (order_id, product_id, quantity) VALUES
(1, 1, 2),
(2, 3, 1); 