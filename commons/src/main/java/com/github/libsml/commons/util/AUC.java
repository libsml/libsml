package com.github.libsml.commons.util;

import java.util.Arrays;

/**
 * Created by yellowhuang on 2015/6/12.
 */
public class AUC {
    public static float auc(float[] positiveProbs, float[] negativeProbs) {

        Arrays.sort(positiveProbs);
        Arrays.sort(negativeProbs);
        int n0 = negativeProbs.length;
        int n1 = positiveProbs.length;
        if (n0 == 0 || n1 == 0) {
            return 0.5f;
        }
        //sacn the data
        int i0 = 0, i1 = 0, rank = 1;
        double rankSum = 0;
        while (i0 < n0 && i1 < n1) {
            float v0 = negativeProbs[i0];
            float v1 = positiveProbs[i1];
            if (v0 < v1) {
                i0++;
                rank++;
            } else if (v1 < v0) {
                i1++;
                rankSum += rank;
                rank++;
            } else {
                float tieScore = v0;
                // how many negatives are tied?
                int k0 = 0;
                while (i0 < n0 && negativeProbs[i0] == tieScore) {
                    k0++;
                    i0++;
                }
                // how many positives are tied?
                int k1 = 0;
                while (i1 < n1 && positiveProbs[i1] == tieScore) {
                    k1++;
                    i1++;
                }
                // we found k0+k1 tied values ranks in [rank, rank+k0+k1)
                rankSum += (rank + (k0 + k1 - 1) / 2.0) * k1;
                rank += k0 + k1;
            }
        }
        if (i1 < n1) {
            rankSum += (rank + (n1 - i1 - 1) / 2.0) * (n1 - i1);
            rank += n1 - i1;
        }
        return (float) ((rankSum / n1 - (n1 + 1) / 2.0) / n0);
    }

    public static void main(String[] args) {
        System.out.println(auc(new float[]{0.9f, 0.8f, 0.6f, 0.55f, 0.54f, 0.01f}, new float[]{0.7f, 0.53f, 0.52f, 0.1f}));
    }
}
