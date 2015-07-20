package com.github.libsml.math;

import com.github.libsml.math.linalg.DenseVector;
import com.github.libsml.math.linalg.Vector;

/**
 * Created by huangyu on 15/7/18.
 */
public class Main {
    public static void main(String[] args) {
        Vector v1 = new DenseVector(new double[]{1, 3, 4});
        Vector v2 = new DenseVector(new double[]{1, 3, 4});
        System.out.println(v1.equals(v2));
    }
}
