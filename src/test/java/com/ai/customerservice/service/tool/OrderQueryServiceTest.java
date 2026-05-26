package com.ai.customerservice.service.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderQueryServiceTest {

    private OrderQueryService orderQueryService;

    @BeforeEach
    void setUp() {
        orderQueryService = new OrderQueryService();
        orderQueryService.init();
    }

    @Test
    void queryOrderStatus_withExistingOrder_shouldReturnOrderDetails() {
        String result = orderQueryService.queryOrderStatus("ORD20260501001");

        assertTrue(result.contains("ORD20260501001"));
        assertTrue(result.contains("已发货"));
        assertTrue(result.contains("智能音箱 Pro"));
        assertTrue(result.contains("338.8"));
    }

    @Test
    void queryOrderStatus_withNonExistentOrder_shouldReturnNotFound() {
        String result = orderQueryService.queryOrderStatus("INVALID_ORDER");

        assertTrue(result.contains("未找到订单号"));
        assertTrue(result.contains("INVALID_ORDER"));
    }

    @Test
    void queryOrderStatus_shouldContainAllItems() {
        String result = orderQueryService.queryOrderStatus("ORD20260501001");

        assertTrue(result.contains("智能音箱 Pro"));
        assertTrue(result.contains("Type-C 数据线"));
    }

    @Test
    void queryOrderLogistics_withShippedOrder_shouldReturnLogistics() {
        String result = orderQueryService.queryOrderLogistics("ORD20260501001");

        assertTrue(result.contains("顺丰速运"));
        assertTrue(result.contains("SF1234567890"));
        assertTrue(result.contains("运输中"));
    }

    @Test
    void queryOrderLogistics_withNonExistentOrder_shouldReturnNotFound() {
        String result = orderQueryService.queryOrderLogistics("INVALID_ORDER");

        assertTrue(result.contains("未找到订单号"));
    }

    @Test
    void queryOrderLogistics_withOrderNoLogistics_shouldReturnNoLogistics() {
        String result = orderQueryService.queryOrderLogistics("ORD20260503003");

        assertTrue(result.contains("暂无物流信息"));
    }

    @Test
    void hiThere_withPendingOrder_shouldReturnWaitingMessage() {
        String result = orderQueryService.hiThere("ORD20260503003");

        assertTrue(result.contains("ORD20260503003"));
        assertTrue(result.contains("加紧处理"));
    }

    @Test
    void hiThere_withNonExistentOrder_shouldReturnNotFound() {
        String result = orderQueryService.hiThere("INVALID_ORDER");

        assertTrue(result.contains("未找到订单号"));
    }

    @Test
    void queryOrderStatus_withCompletedOrder_shouldReturnDetails() {
        String result = orderQueryService.queryOrderStatus("ORD20260502002");

        assertTrue(result.contains("已完成"));
        assertTrue(result.contains("智能手环 Lite"));
        assertTrue(result.contains("159"));
    }

    @Test
    void queryOrderStatus_withCancelledOrder_shouldReturnDetails() {
        String result = orderQueryService.queryOrderStatus("ORD20260504004");

        assertTrue(result.contains("已取消"));
        assertTrue(result.contains("无线耳机 Air"));
    }
}
