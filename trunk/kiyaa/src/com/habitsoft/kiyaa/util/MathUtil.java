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
     * Supports either 0, 1, 2, -1, -2, or -3 decimal places.
     */
    public static long roundToFp(double val, int decimalPlaces) {
        switch(decimalPlaces) {
        case 0: return round(val);
        case 1: return round(val*10.0);
        case 2: return round(val*100.0);
        case -1: return round(val*0.1);
        case -2: return round(val*0.01);
        case -3: return round(val*0.001);
        default:
            throw new IllegalArgumentException("Unsupported number of decimal places: "+decimalPlaces);
        }
    }
    
    public static double longToDouble(long val, int decimalPlaces) {
        switch(decimalPlaces) {
        case 0: return (double)val;
        case 1: return (double)val/10.0;
        case 2: return (double)val/100.0;
        case -1: return (double)val/0.1;
        case -2: return (double)val/0.01;
        case -3: return (double)val/0.001;
        default:
            throw new IllegalArgumentException("Unsupported number of decimal places: "+decimalPlaces);
        }
    }
}
