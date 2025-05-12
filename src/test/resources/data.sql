INSERT INTO product (name, description, price, count, img_path) VALUES
('Test Product 1', 'Description 1', 99.99, 10, '/images/product1.jpg'),
('Test Product 2', 'Description 2', 149.99, 5, '/images/product2.jpg'),
('Test Product 3', 'Description 3', 199.99, 0, '/images/product3.jpg');

INSERT INTO customer_order (total_sum) VALUES
(249.98),
(199.99);

INSERT INTO order_product (order_id, product_id, quantity) VALUES
(1, 1, 2),
(2, 3, 1); 