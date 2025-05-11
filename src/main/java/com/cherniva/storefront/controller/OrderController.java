package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.CustomerOrder;
import com.cherniva.storefront.model.OrderProduct;
import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.OrderProductRepo;
import com.cherniva.storefront.repository.CustomerOrderRepository;
import com.cherniva.storefront.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class OrderController {
    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderProductRepo orderProductRepo;

    public OrderController(CustomerOrderRepository orderRepository, ProductRepository productRepository, OrderProductRepo orderProductRepo) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderProductRepo = orderProductRepo;
    }

    @PostMapping("/buy")
    public String placeOrder(Model model) {
        List<Product> productsInCart = productRepository.getProductsByCountGreaterThanZero();
        BigDecimal totalAmount = getTotalAmount(productsInCart);

        CustomerOrder order = new CustomerOrder();
        order.setTotalSum(totalAmount);

        CustomerOrder savedOrder = orderRepository.save(order);

        List<OrderProduct> orderProducts = productsInCart.stream()
                .map(p -> productToOrderProduct(p, savedOrder))
                .toList();

        orderProductRepo.saveAll(orderProducts);

        savedOrder.setProducts(orderProducts);

        for (Product product : productsInCart) {
            product.setCount(0);
            productRepository.save(product);
        }

        model.addAttribute("newOrder", true);
        model.addAttribute("order", savedOrder);

        return "order";
    }

    @GetMapping("/orders")
    public String getOrders(Model model) {
        List<CustomerOrder> orders = orderRepository.findAll();

        model.addAttribute("orders", orders);
        model.addAttribute("numOrders", orders.size());
        model.addAttribute("totalSum", getTotalSum(orders));

        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String getOrder(Model model,
                           @PathVariable("id") Long id) {
        CustomerOrder order = orderRepository.getReferenceById(id);

        model.addAttribute("newOrder", false);
        model.addAttribute("order", order);

        return "order";
    }

    private BigDecimal getTotalAmount(List<Product> productsInCart) {
        return productsInCart.stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getTotalSum(List<CustomerOrder> orders) {
        return orders.stream()
                .map(CustomerOrder::getTotalSum)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderProduct productToOrderProduct(Product product, CustomerOrder order) {
        OrderProduct orderProduct = new OrderProduct();
        orderProduct.setProduct(product);
        orderProduct.setOrder(order);
        orderProduct.setQuantity(product.getCount());
        return orderProduct;
    }
}
