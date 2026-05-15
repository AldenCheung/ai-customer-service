package com.ai.customerservice.service.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderQueryService {

    private static final Logger log = LoggerFactory.getLogger(OrderQueryService.class);
    private static final String ORDERS_FILE = "orders.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Map<String, Object>> orderMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource(ORDERS_FILE);
            try (InputStream is = resource.getInputStream()) {
                List<Map<String, Object>> orders = objectMapper.readValue(is, new TypeReference<>() {});
                for (Map<String, Object> order : orders) {
                    String orderId = (String) order.get("orderId");
                    orderMap.put(orderId, order);
                }
                log.info("Loaded {} orders from {}", orderMap.size(), ORDERS_FILE);
            }
        } catch (Exception e) {
            log.error("Failed to load orders from {}", ORDERS_FILE, e);
        }
    }

    @Tool("查询订单的状态")
    public String queryOrderStatus(@P("订单号") String orderId) {
        log.info("Tool called: queryOrderStatus({})", orderId);
        Map<String, Object> order = orderMap.get(orderId);
        if (order == null) {
            return "未找到订单号为 " + orderId + " 的订单，请核实订单号是否正确";
        }
        String status = (String) order.get("status");
        String orderTime = (String) order.get("orderTime");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
        StringBuilder sb = new StringBuilder();
        sb.append("订单号: ").append(orderId).append("\n");
        sb.append("状态: ").append(status).append("\n");
        sb.append("下单时间: ").append(orderTime).append("\n");
        sb.append("商品明细:\n");
        for (Map<String, Object> item : items) {
            sb.append("  - ").append(item.get("productName"))
                    .append(" x").append(item.get("quantity"))
                    .append("  ¥").append(item.get("unitPrice")).append("\n");
        }
        sb.append("总金额: ¥").append(order.get("totalAmount"));
        return sb.toString();
    }

    @Tool("当订单状态为已发货，调用此方法查询物流信息")
    public String queryOrderLogistics(@P("订单号") String orderId) {
        log.info("Tool called: queryOrderLogistics({})", orderId);
        Map<String, Object> order = orderMap.get(orderId);
        if (order == null) {
            return "未找到订单号为 " + orderId + " 的订单";
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> logistics = (Map<String, Object>) order.get("logistics");
        if (logistics == null) {
            return "订单 " + orderId + " 暂无物流信息";
        }
        return "物流公司: " + logistics.get("company")
                + "，运单号: " + logistics.get("trackingNo")
                + "，当前状态: " + logistics.get("currentStatus");
    }

    @Tool("当订单状态为待发货，调用此方法")
    public String hiThere(@P("订单号") String orderId) {
        log.info("Tool called: hiThere({})", orderId);
        Map<String, Object> order = orderMap.get(orderId);
        if (order == null) {
            return "未找到订单号为 " + orderId + " 的订单";
        }
        return "小二正在加紧处理您的订单（" + orderId + "），请耐心等待^_^";
    }
}
