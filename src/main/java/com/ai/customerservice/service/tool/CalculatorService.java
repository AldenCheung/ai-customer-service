package com.ai.customerservice.service.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CalculatorService {

    private static final Logger log = LoggerFactory.getLogger(CalculatorService.class);

    @Tool("计算两个数的加法")
    public double add(@P("第一个数") double a, @P("第二个数") double b) {
        log.info("Tool called: add({}, {})", a, b);
        return a + b;
    }

    @Tool("计算两个数的减法")
    public double subtract(@P("被减数") double a, @P("减数") double b) {
        log.info("Tool called: subtract({}, {})", a, b);
        return a - b;
    }

    @Tool("计算两个数的乘法")
    public double multiply(@P("第一个数") double a, @P("第二个数") double b) {
        log.info("Tool called: multiply({}, {})", a, b);
        return a * b;
    }

    @Tool("计算两个数的除法")
    public String divide(@P("被除数") double a, @P("除数") double b) {
        log.info("Tool called: divide({}, {})", a, b);
        if (b == 0) {
            return "错误：除数不能为零";
        }
        return String.valueOf(a / b);
    }

    @Tool("计算一个数的幂次方")
    public double power(@P("底数") double base, @P("指数") double exponent) {
        log.info("Tool called: power({}, {})", base, exponent);
        return Math.pow(base, exponent);
    }

    @Tool("计算一个数的平方根")
    public String sqrt(@P("要计算平方根的数") double number) {
        log.info("Tool called: sqrt({})", number);
        if (number < 0) {
            return "错误：不能对负数求平方根";
        }
        return String.valueOf(Math.sqrt(number));
    }
}
