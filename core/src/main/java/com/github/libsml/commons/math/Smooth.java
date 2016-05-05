package com.github.libsml.commons.math;

import com.github.libsml.commons.util.PrintUtils;
import com.github.libsml.commons.util.VecUtils;
import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.IOException;

import static com.github.libsml.commons.math.Digamma.digamma;

/**
 * Created by huangyu on 2015/6/16.
 */
public class Smooth {


    public static void bayesianSmooth(double[] clicks, double[] impressions, double[] alphaBeta, int len) {
        Preconditions.checkArgument(clicks != null && impressions != null && clicks.length == impressions.length, "Bayesian smooth exception.");
        Preconditions.checkArgument(alphaBeta != null && alphaBeta.length == 2, "Bayesian smooth exception.");
        for (int i = 0; i < clicks.length; i++) {
            Preconditions.checkArgument(clicks[i] >= 0 && impressions[i] >= 0 && clicks[i] <= impressions[i],
                    "Bayesian smooth exception:click=" + clicks[i] + ",impression=" + impressions[i]);
        }
        double tmp1, tmp2;
        double updateAlpha, updateBeta;
//        alphaBeta[0] = VecUtils.avg(clicks, 0, len);
//        alphaBeta[1] = VecUtils.avg(impressions, 0, len) - alphaBeta[0];
        alphaBeta[0] = 1;
        alphaBeta[1] = 1;
        int iter = 0;
        do {

            if (alphaBeta[0] <= 1E-10 || alphaBeta[1] <= 1E-10) {
                break;
            }

            tmp1 = 0;
            tmp2 = 0;
            for (int i = 0; i < len; i++) {

                tmp1 += digamma(alphaBeta[0] + clicks[i]) - digamma(alphaBeta[0]);
                tmp2 += digamma(alphaBeta[0] + alphaBeta[1] + impressions[i]) - digamma(alphaBeta[0] + alphaBeta[1]);
            }
            updateAlpha = tmp1 / tmp2;
            alphaBeta[0] *= updateAlpha;

            tmp1 = 0;
            tmp2 = 0;
            for (int i = 0; i < len; i++) {
                tmp1 += digamma(alphaBeta[1] + impressions[i] - clicks[i]) - digamma(alphaBeta[1]);
//                System.out.println(String.format("beta=%f,imp=%f,click=%f,digamma=%f,tmp1=%f"
//                        , alphaBeta[1], impressions[i], clicks[i],
//                        digamma(alphaBeta[1] + impressions[i] - clicks[i]) - digamma(alphaBeta[1]),
//                        tmp1));
                tmp2 += digamma(alphaBeta[0] + alphaBeta[1] + impressions[i]) - digamma(alphaBeta[0] + alphaBeta[1]);
            }
            updateBeta = tmp1 / tmp2;
            alphaBeta[1] *= updateBeta;
            iter++;
//            System.out.println(updateAlpha + ":" + updateBeta);
            System.out.println("iter:" + iter + "  " + alphaBeta[0] + ":" + alphaBeta[1]);
        } while (iter <= 10000 && (Math.abs(updateAlpha - 1) > 1E-10 || Math.abs(updateBeta - 1) > 1E-10));
    }


    public static void main(String[] args) throws IOException {

        int n = 1843036;
        double[] clicks = new double[n];
        double[] impressions = new double[n];
        double[] alphaBeta = new double[2];
        LineIterator lt = FileUtils.lineIterator(new File("data/bayesian"));
        int index = 0;
        while (lt.hasNext()) {
            String[] ss = lt.next().split("\\s+");
            clicks[index] = Double.parseDouble(ss[1]);
            impressions[index] = Double.parseDouble(ss[2]);
//            if (clicks[index] == impressions[index]) {
//                continue;
//            }
            index++;
        }
        bayesianSmooth(clicks, impressions, alphaBeta, index);
        PrintUtils.printArray(alphaBeta);
    }
}
