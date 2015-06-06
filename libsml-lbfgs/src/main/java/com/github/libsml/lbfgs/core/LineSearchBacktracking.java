package com.github.libsml.lbfgs.core;


import com.github.libsml.function.LossFunction;

import static com.github.libsml.commons.util.VecUtils.*;

public class LineSearchBacktracking implements LineSearchFunction {

    public int lineSearch(int n, float[] x, float[] f, float[] g, float[] s,
                          float[] stp, float[] xp, float[] gp,  int k, LossFunction loss,
                          LBFGSParameter param) {

        int count = 0;
        float width, dg;
        float finit, dginit = 0f, dgtest;
        final float dec = 0.5f, inc = 2.1f;

		/* Check the input parameters for errors. */
        if (stp[0] <= 0) {
            throw new IllegalStateException("LBFGSERR_INVALIDPARAMETERS");
        }

		/* Compute the initial gradient in the search direction. */
        dginit = vecdot(g, s);

		/* Make sure that s points to a descent direction. */
        if (dginit > 0) {
            throw new IllegalStateException("LBFGSERR_INCREASEGRADIENT");
        }

		/* The initial value of the objective function. */
        finit = f[0];
        dgtest = param.ftol * dginit;
        while (true) {
            veccpy(x, xp);
            vecadd(x, s, stp[0]);

			/* Evaluate the function and gradient values. */
            f[0] = loss.lossAndGrad(x, g, k);
            // f[0] += param.orthantwiseC
            // * LBFGS.L2Norm(x);

            count++;
            if (f[0] > finit + stp[0] * dgtest) {
//				System.out.println("fx:"+f[0]+",dt:"+(finit + stp[0] * dgtest));
                width = dec;
            } else {
                /* The sufficient decrease condition (Armijo condition). */
                if (param.linesearch == LineSearchConstant.LBFGS_LINESEARCH_BACKTRACKING_ARMIJO
                        || param.linesearch == LineSearchConstant.LBFGS_LINESEARCH_BACKTRACKING) {
					/* Exit with the Armijo condition. */
                    return count;
                }
				/* Check the Wolfe condition. */
                dg = vecdot(g, s);
                if (dg < param.wolfe * dginit) {
                    width = inc;
                } else {
                    if (param.linesearch == LineSearchConstant.LBFGS_LINESEARCH_BACKTRACKING_WOLFE) {
						/* Exit with the regular Wolfe condition. */
                        return count;
                    }

					/* Check the strong Wolfe condition. */
                    if (dg > -param.wolfe * dginit) {
                        width = dec;
                    } else {
						/* Exit with the strong Wolfe condition. */
                        return count;
                    }
                }
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
}
