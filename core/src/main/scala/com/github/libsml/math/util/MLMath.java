package com.github.libsml.math.util;

/**
 * Created by huangyu on 15/8/17.
 */
public class MLMath {

    /**
     * When `x` is positive and large, computing `math.log(1 + math.exp(x))` will lead to arithmetic
     * overflow. This will happen when `x > 709.78` which is not a very large number.
     * It can be addressed by rewriting the formula into `x + math.log1p(math.exp(-x))` when `x > 0`.
     *
     * @param x a floating-point value as input.
     * @return the result of `math.log(1 + math.exp(x))`.
     */
    public static double log1pExp(double x) {
        if (x > 0) {
            return x + Math.log1p(Math.exp(-x));
        } else {
            return Math.log1p(Math.exp(x));
        }
    }

}
