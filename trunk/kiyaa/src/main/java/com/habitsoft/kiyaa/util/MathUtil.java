package com.habitsoft.kiyaa.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtil {

	final static BigDecimal GLOBAL_BIGDECIMAL_SIZE_LIMIT = new BigDecimal(10000000000000000L);
	final static int GLOBAL_BIGDECIMAL_SCALE_LIMIT = 10;

    public static boolean validBigDecimal(BigDecimal result) {
		if (result.scale() > GLOBAL_BIGDECIMAL_SCALE_LIMIT || result.abs().compareTo(GLOBAL_BIGDECIMAL_SIZE_LIMIT) > 0) {
			return false;
		} else return true;		
    }
    
    /**
     * Apply rounding such that:
     * 
     * round(4.5) == 5L
     * round(-4.5) == -5L
     * round(4.49) == 4L
     * round(3.51) == 4L
     * round(-4.49) == -4L
     * round(-3.51) == -4L
     * 
     */
    public static long round(double val) {
        return val >= 0?Math.round(val):-Math.round(Math.abs(val));
    }
    
    /**
     * Return a long value representing a fixed-point value with the
     * given number of decimal places.
     * 
     * roundTo(x, 0) == round(x)
     * roundTo(4, 1) == 40L
     * roundTo(4.45, 1) == 45L
     * roundTo(-4.45, 1) == -45L
     * roundTo(-4.455, 2) == -446L
     * roundTo(4.455, 2) == 446L
     * 
     * Supports either 0, 1, 2, 3, -1, -2, or -3 decimal places.
     * 
     * Half-integers will be rounded up.
     */
    public static long roundToFixedPoint(double val, int decimalPlaces) {
    	return round(pow10(val, decimalPlaces));
    }
    
    /**
     * Round a BigDecimal to its long value, remove all decimals using ROUND_HALF_UP
     * Note the BigDecimal amounts used in our system is scaled to the same as the long values, so we wont need to know the decimal place we need here.
     * 
     * @param val
     * @return
     */
    public static long roundBigDecimalToLong(BigDecimal val) {
    	return val.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /**
     * Raise the number by the given power of 10.
     * 
     * roundTo(x, 0) == 0
     * roundTo(4, 1) == 40
     * roundTo(4.45, 1) == 44.5
     * roundTo(-4.45, 1) == -4.45
     * roundTo(-4.455, 2) == -4.455
     * roundTo(4.455, 2) == 4.455
     * 
     * Supports either 0, 1, 2, 3, -1, -2, or -3 powers.
     */
    public static double pow10(double val, int decimalPlaces) {
        switch(decimalPlaces) {
        case 0: return (val);
        case 1: return (val*10.0);
        case 2: return (val*100.0);
        case 3: return (val*1000.0);
        case -1: return (val*0.1);
        case -2: return (val*0.01);
        case -3: return (val*0.001);
        default:
            throw new IllegalArgumentException("Unsupported number of decimal places: "+decimalPlaces);
        }
    }
    
    /**
     * Convert a fixed-point value as returned by roundToFixedPoint()
     * back to a double floating-point value.
     */
    public static double fixedPointToDouble(long val, int decimalPlaces) {
        switch(decimalPlaces) {
        case 0: return (double)val;
        case 1: return (double)val/10.0;
        case 2: return (double)val/100.0;
        case 3: return (double)val/1000.0;
        case -1: return (double)val/0.1;
        case -2: return (double)val/0.01;
        case -3: return (double)val/0.001;
        default:
            throw new IllegalArgumentException("Unsupported number of decimal places: "+decimalPlaces);
        }
    }
    
    /**
     * Return a number whose absolute value is less or
     * equal than value, and which is as close or closer
     * to zero as limit in the same direction.
     * 
     * i.e.
     * 
     * When limit is positive, return the same as Math.min(value, limit).
     * When limit is negative return the same as Math.max(value, limit).
     * When limit is zero, return zero.
     * 
     * If the 'value' is negative, the result will be negative
     * or zero.  If the 'value' is positive, the result will be
     * positive or zero.
     * 
     * This is used to restrict value so that it does not exceed
     * limit, but supporting a negative number better than Math.min()
     * would.
     * 
     * For example, a refund or a vendor credit might be recorded
     * as a negative amount, and we just want to ensure that a
     * payment amount does not exceed that limit even though it is
     * negative.  If we just used Math.min() we'd always adjust our
     * value to the maximum allocation if the limit was negative.
     */
    public static long minAmount(long value, long limit) {
    	if(limit != 0 && (limit > 0) == (value < limit)) return value;
    	else return limit;
    }
}
