package com.habitsoft.kiyaa.util;

import com.habitsoft.kiyaa.util.MathUtil;

import junit.framework.TestCase;

public class TestMathUtil extends TestCase {

    public void testRounding() {
        assertEquals(5L, MathUtil.round(5.0));
        assertEquals(5L, MathUtil.round(4.5));
        assertEquals(-5L, MathUtil.round(-5.0));
        assertEquals(-5L, MathUtil.round(-4.5));
        assertEquals(-4L, MathUtil.round(-3.51));
        assertEquals(-3L, MathUtil.round(-3.49));
        
        assertEquals(-3L, MathUtil.roundToFp(-3.49, 0));
        assertEquals(40L, MathUtil.roundToFp(4.0, 1));
        assertEquals(45L, MathUtil.roundToFp(4.45, 1));
        assertEquals(-45L, MathUtil.roundToFp(-4.45, 1));
        assertEquals(-446L, MathUtil.roundToFp(-4.455, 2));
        assertEquals(446L, MathUtil.roundToFp(4.455, 2));
    }
    
    public void testLongToDouble() {
        assertEquals(1.23, MathUtil.longToDouble(123, 2));
        assertEquals(-1.23, MathUtil.longToDouble(-123, 2));
        assertEquals(1.6, MathUtil.longToDouble(16, 1));
        assertEquals(-1.6, MathUtil.longToDouble(-16, 1));
        assertEquals(6.0, MathUtil.longToDouble(6, 0));
        assertEquals(-6.0, MathUtil.longToDouble(-6, 0));
    }
}
