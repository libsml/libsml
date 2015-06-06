package com.github.libsml.lbfgs.core;


import com.github.libsml.commons.util.VecUtils;
import com.github.libsml.function.LossFunction;

public class LineSearchOwlqn implements LineSearchFunction {

    public int lineSearch(int n, float[] x, float[] f, float[] g, float[] s,
                          float[] stp, float[] xp, final float[] gp,  int k,
                          LossFunction loss, LBFGSParameter param) {

        int i, count = 0;
        float width = 0.5f, norm = 0f;
        float finit = f[0], dgtest;

        if (stp[0] <= 0) {
            throw new IllegalStateException("LBFGSERR_INVALIDPARAMETERS");
        }

		/* Choose the orthant for the new point. */
        // wp.clear();
        // xp.forEachPair(new IntFloatProcedure() {
        //
        // public boolean apply(int first, float second) {
        // wp.put(first, second == 0 ? -gp.get(first) : second);
        // return true;
        // }
        // });


//        for (i = 0; i < n; ++i) {
//            wp[i] = (xp[i] == 0.) ? -gp[i] : xp[i];
//        }


        while (true) {
            /* Update the current point. */
            VecUtils.veccpy(x, xp);
            VecUtils.vecadd(x, s, stp[0]);

			/* The current point is projected onto the orthant. */
//            owlqnProject(x, wp, param.orthantwiseStart, param.orthantwiseEnd);
            owlqnProject(x, xp, gp, param.orthantwiseStart, param.orthantwiseEnd);

			/* Evaluate the function and gradient values. */
            f[0] = loss.lossAndGrad(x, g, k);

			/*
             * Compute the L1 norm of the variables and add it to the object
			 * value.
			 */
            norm = VecUtils.owlqnL1Norm(x, param.orthantwiseStart, param.orthantwiseEnd);
            f[0] += norm * param.orthantwiseC;
            count++;

            dgtest = 0;


            for (i = 0; i < n; ++i) {
                dgtest += (x[i] - xp[i]) * gp[i];
            }

            if (f[0] <= finit + param.ftol * dgtest) {
                return count;
            }
            if (stp[0] < param.minStep) {
                throw new IllegalStateException("LBFGSERR_MINIMUMSTEP");
            }
            if (stp[0] > param.maxStep) {
				/* The step is the maximum value. */
                throw new IllegalStateException("LBFGSERR_MAXIMUMSTEP");
            }
            if (param.maxLinesearch <= count) {
				/* Maximum number of iteration. */
                throw new IllegalStateException("LBFGSERR_MAXIMUMLINESEARCH");
            }
            stp[0] *= width;

        }

    }

    private void owlqnProject(float[] d, float[] sign, int start, int end) {

        // for (int i = start; i < end; i++) {
        // if (d.containsKey(i) && sign.containsKey(i)) {
        // if (d.get(i) * sign.get(i) <= 0) {
        // d.removeKey(i);
        // }
        // }
        // }

        for (int i = start; i < end; ++i) {
            if (d[i] * sign[i] <= 0) {
                d[i] = 0;
            }
        }
    }

    private void owlqnProject(float[] d, float[] x, float[] pg, int start, int end) {


        for (int i = start; i < end; ++i) {
            float sign = (x[i] == 0.) ? -pg[i] : x[i];
            if (d[i] * sign <= 0) {
                d[i] = 0;
            }
        }
    }


}
