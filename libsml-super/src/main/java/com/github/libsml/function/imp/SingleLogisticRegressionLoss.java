package com.github.libsml.function.imp;


import com.github.libsml.commons.util.PrintUtils;
import com.github.libsml.commons.util.VecUtils;
import com.github.libsml.data.liblinear.Feature;
import com.github.libsml.data.liblinear.Problem;
import com.github.libsml.function.LossFunction;

import java.util.Arrays;

public class SingleLogisticRegressionLoss implements LossFunction {


    private final Problem prob;
    private final float c;
    private double[] D;
    private final double[] tmp;

    public SingleLogisticRegressionLoss(Problem prob, float c) {
        this.prob = prob;
        this.c = c;
        tmp = new double[prob.n];

    }

    private double xv(Feature[] x, float[] v) {
        double xv = 0;
        for (Feature f : x) {
            xv += v[f.getIndex()] * f.getValue();
        }
        return xv;
    }

    private void xTv(Feature[] x, float v, float[] xTv) {
        for (Feature f : x) {
            xTv[f.getIndex()] += v * f.getValue();
        }
    }

    private void xTv(Feature[] x, double v, double[] xTv) {
        for (Feature f : x) {
            xTv[f.getIndex()] += v * f.getValue();
        }
    }


    @Override
    public float lossAndGrad(float[] w, float[] g, int k) {

//        Arrays.fill(g, 0f);
        Arrays.fill(tmp, 0);
        Feature[][] x = prob.x;
        float[] y = prob.y;
        float[] wt = prob.weight;

        double fx = 0f;


        for (int i = 0; i < x.length; i++) {
            double yz = xv(x[i], w) * y[i];

            if (yz >= 0)
                fx += Math.log(1 + Math.exp(-yz))*wt[i];
            else
                fx += (-yz + Math.log(1 + Math.exp(yz)))*wt[i];

            double z = 1 / (1 + Math.exp(-yz));

            xTv(x[i], y[i] * (z - 1) * wt[i], tmp);

        }

//        System.arraycopy(tmp,0,g,0,g.length);

        //L2 norm
        if (c > 0) {
            fx = c * VecUtils.vecdot(w, w) / 2.0 + fx;
            VecUtils.vecadd(tmp, w, c);
        }

        VecUtils.arrayCopy(tmp, g);

        return (float) fx;
    }


    /**
     * Method for trust region newton method
     *
     * @param w
     * @param d
     * @param Hs
     */
    public void Hv(float[] w, float[] d, float[] Hs, int k, int cgIter) {
        int i;
        int l = prob.l;
        Feature[][] x = prob.x;
        float[] y = prob.y;

//        Arrays.fill(Hs,0f);
        Arrays.fill(tmp, 0f);

        if (D == null) {
            D = new double[l];
        }
        for (i = 0; i < l; i++) {
            if (cgIter == 1) {
                D[i] = (1 / (1 + Math.exp(-xv(x[i], w) * y[i])));
                D[i] = D[i] * (1 - D[i]);
            }
            xTv(x[i], xv(x[i], d) * prob.weight[i] * D[i], tmp);

        }

//        System.arraycopy(tmp,0,Hs,0,Hs.length);

        VecUtils.arrayCopy(tmp, Hs);
        //L2 norm
        if (c > 0) {
            VecUtils.vecadd(Hs, d, c);
        }
    }


}
