package com.github.libsml.function.imp;



import com.github.libsml.data.liblinear.Feature;
import com.github.libsml.data.liblinear.Problem;
import com.github.libsml.function.EvaluatorFunction;
import com.github.libsml.model.Statistics;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by yellowhuang on 2015/4/17.
 */
public class SingleLogisticRegressionEvaluator implements EvaluatorFunction {

    private final Problem prob;
    private final float threshold;

    public SingleLogisticRegressionEvaluator(Problem prob, float threshold) {
        if (threshold < 0) {
            throw new IllegalStateException("Evaluator exception:threshold=" + threshold + " is less than 0");
        }
        this.prob = prob;
        this.threshold = threshold;

    }

    @Override
    public Statistics evaluate(float[] w, int k) {

        int tp = 0, tn = 0, fp = 0, fn = 0;


        Feature[][] x = prob.x;
        float[] y = prob.y;
        double[][] ps = new double[y.length][2];

        for (int i = 0; i < x.length; i++) {
            double xv = 0;
            for (Feature f : x[i]) {
                xv += w[f.getIndex()] * f.getValue();
            }
            double p = 1 / (Math.exp(-xv) + 1);
            if (p >= threshold) {
                if (y[i] == 1) {
                    tp++;
                } else {
                    fp++;
                }
            } else {
                if (y[i] == -1) {
                    tn++;
                } else {
                    fn++;
                }
            }
            ps[i][0] = p;
            ps[i][1] = y[i];
        }
        float accuracy = (float) (tp + tn) / (tp + tn + fp + fn);
        float precision = (float) tp / (tp + fp);
        float recall = (float) tp / (tp + fn);
        float f1Score = (2 * precision * recall) / (precision + recall);
        Arrays.sort(ps, new Comparator<double[]>() {
            @Override
            public int compare(double[] o1, double[] o2) {
                int c = -Double.compare(o1[0], o2[0]);
                if (c == 0) {
                    return Double.compare(o1[1], o2[1]);
                }
                return c;
            }
        });

        int pn = tp + fn;
        int nn = tn + fp;
        int all = pn * nn;
        int aucCount = 0;

        for (double[] p : ps) {
            if (p[1] == -1) {
                nn--;
            } else {
                aucCount += nn;
            }
        }
        float auc = all == 0 ? 0 : (float) aucCount / all;

        return new Statistics(precision, recall, f1Score, auc, accuracy);

    }
}
