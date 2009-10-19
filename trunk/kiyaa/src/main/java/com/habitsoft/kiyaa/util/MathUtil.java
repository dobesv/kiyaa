package com.habitsoft.kiyaa.util;

public class MathUtil {

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
}
