package com.github.libsml.commons.math;

import org.apache.commons.math3.special.Gamma;

/**
 * Created by yellowhuang on 2015/6/16.
 */
public class Digamma {
    public static double digamma(double x) {
        double r = 0.0;

        while (x <= 5) {
            r -= 1 / x;
            x += 1;
        }

        double f = 1.0 / (x * x);
        double t = f * (-1.0 / 12.0 + f * (1.0 / 120.0 + f * (-1.0 / 252.0 + f * (1.0 / 240.0
                + f * (-1.0 / 132.0 + f * (691.0 / 32760.0 + f * (-1.0 / 12.0 + f * 3617.0 / 8160.0)))))));
        return r + Math.log(x) - 0.5 / x + t;
    }

    public static void main(String[] args){
        System.out.println(Gamma.gamma(3.4));
//        Gamma.gamma(3.4);
    }
}
