package com.habitsoft.kiyaa.test.util;

import junit.framework.TestCase;

import com.habitsoft.kiyaa.util.MathUtil;

public class GwtTestMathUtil extends TestCase {

    public void testRounding() {
        assertEquals(5L, MathUtil.round(5.0));
        assertEquals(5L, MathUtil.round(4.5));
        assertEquals(-5L, MathUtil.round(-5.0));
        assertEquals(-5L, MathUtil.round(-4.5));
        assertEquals(-4L, MathUtil.round(-3.51));
        assertEquals(-3L, MathUtil.round(-3.49));
        
        assertEquals(-3L, MathUtil.roundToFixedPoint(-3.49, 0));
        assertEquals(40L, MathUtil.roundToFixedPoint(4.0, 1));
        assertEquals(45L, MathUtil.roundToFixedPoint(4.45, 1));
        assertEquals(-45L, MathUtil.roundToFixedPoint(-4.45, 1));
        assertEquals(-446L, MathUtil.roundToFixedPoint(-4.455, 2));
        assertEquals(446L, MathUtil.roundToFixedPoint(4.455, 2));
    }
    
    public void testLongToDouble() {
        assertEquals(1.23, MathUtil.fixedPointToDouble(123, 2));
        assertEquals(-1.23, MathUtil.fixedPointToDouble(-123, 2));
        assertEquals(1.6, MathUtil.fixedPointToDouble(16, 1));
        assertEquals(-1.6, MathUtil.fixedPointToDouble(-16, 1));
        assertEquals(6.0, MathUtil.fixedPointToDouble(6, 0));
        assertEquals(-6.0, MathUtil.fixedPointToDouble(-6, 0));
    }

    public void testMinAmount() {
    	assertEquals(2, MathUtil.minAmount(2, 2));
    	assertEquals(1, MathUtil.minAmount(1, 2));
    	assertEquals(2, MathUtil.minAmount(3, 2));
    	assertEquals(2, MathUtil.minAmount(2, 3));
    	assertEquals(-1, MathUtil.minAmount(-1, 2));
    	assertEquals(-1, MathUtil.minAmount(-1, -2));
    	assertEquals(-2, MathUtil.minAmount(-2, 2));
    	assertEquals(-3, MathUtil.minAmount(-3, 2));
    	assertEquals(2, MathUtil.minAmount(2, -3));
    	assertEquals(-2, MathUtil.minAmount(-3, -2));
    	assertEquals(0, MathUtil.minAmount(5, 0));
    	assertEquals(0, MathUtil.minAmount(-5, 0));
    }
}
