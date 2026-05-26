package com.ai.customerservice.service.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorServiceTest {

    private CalculatorService calculatorService;

    @BeforeEach
    void setUp() {
        calculatorService = new CalculatorService();
    }

    @Test
    void add_shouldReturnSum() {
        assertEquals(5.0, calculatorService.add(2, 3));
    }

    @Test
    void add_withNegativeNumbers_shouldReturnSum() {
        assertEquals(-1.0, calculatorService.add(2, -3));
    }

    @Test
    void add_withZero_shouldReturnSameValue() {
        assertEquals(5.0, calculatorService.add(5, 0));
    }

    @Test
    void add_withDecimalNumbers_shouldReturnSum() {
        assertEquals(0.3, calculatorService.add(0.1, 0.2), 0.0001);
    }

    @Test
    void subtract_shouldReturnDifference() {
        assertEquals(2.0, calculatorService.subtract(5, 3));
    }

    @Test
    void subtract_withNegativeResult_shouldReturnNegative() {
        assertEquals(-3.0, calculatorService.subtract(2, 5));
    }

    @Test
    void multiply_shouldReturnProduct() {
        assertEquals(6.0, calculatorService.multiply(2, 3));
    }

    @Test
    void multiply_withZero_shouldReturnZero() {
        assertEquals(0.0, calculatorService.multiply(5, 0));
    }

    @Test
    void multiply_withNegativeNumber_shouldReturnNegative() {
        assertEquals(-6.0, calculatorService.multiply(2, -3));
    }

    @Test
    void divide_shouldReturnQuotient() {
        assertEquals("2.5", calculatorService.divide(5, 2));
    }

    @Test
    void divide_byZero_shouldReturnErrorMessage() {
        assertEquals("错误：除数不能为零", calculatorService.divide(5, 0));
    }

    @Test
    void divide_exactDivision_shouldReturnWholeNumber() {
        assertEquals("2.0", calculatorService.divide(10, 5));
    }

    @Test
    void power_shouldReturnPowerResult() {
        assertEquals(8.0, calculatorService.power(2, 3));
    }

    @Test
    void power_withZeroExponent_shouldReturnOne() {
        assertEquals(1.0, calculatorService.power(5, 0));
    }

    @Test
    void power_withNegativeExponent_shouldReturnFraction() {
        assertEquals(0.25, calculatorService.power(2, -2));
    }

    @Test
    void sqrt_shouldReturnSquareRoot() {
        double result = Double.parseDouble(calculatorService.sqrt(9));
        assertEquals(3.0, result, 0.0001);
    }

    @Test
    void sqrt_ofZero_shouldReturnZero() {
        assertEquals("0.0", calculatorService.sqrt(0));
    }

    @Test
    void sqrt_ofNegativeNumber_shouldReturnErrorMessage() {
        assertEquals("错误：不能对负数求平方根", calculatorService.sqrt(-1));
    }

    @Test
    void sqrt_ofDecimalNumber_shouldReturnSquareRoot() {
        double result = Double.parseDouble(calculatorService.sqrt(2));
        assertEquals(Math.sqrt(2), result, 0.0001);
    }
}
