package com.cherniva.storefront.utils;

import com.cherniva.storefront.model.CustomerOrder;

import java.math.BigDecimal;
import java.util.List;

public class OrderUtils {
    public static BigDecimal getTotalSum(List<CustomerOrder> orders) {
        return orders.stream()
                .map(CustomerOrder::getTotalSum)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
