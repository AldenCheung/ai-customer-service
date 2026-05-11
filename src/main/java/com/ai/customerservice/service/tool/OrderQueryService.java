package com.ai.customerservice.service.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OrderQueryService {
    private static final Logger log = LoggerFactory.getLogger(OrderQueryService.class);

    @Tool("查询订单的状态")
    public String queryOrderStatus(@P("订单号") String orderId){
        System.out.println("触发Tool！");
        return "已发货";
    }

    @Tool("当订单状态为已发货，调用此方法")
    public String queryOrderLogistics(@P("订单号") String orderId){
        System.out.println("触发链式调用！");
        return "您的包裹待揽收";
    }

    @Tool("当订单状态为待发货，调用此方法")
    public String hiThere(@P("订单号") String orderId){
        return "小二正在加紧处理您的订单，请耐心等待^_^";
    }
}
