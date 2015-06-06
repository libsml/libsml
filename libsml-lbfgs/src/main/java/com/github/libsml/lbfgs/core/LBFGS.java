package com.github.libsml.lbfgs.core;

import com.github.libsml.commons.util.VecUtils;
import com.github.libsml.function.EvaluatorFunction;
import com.github.libsml.function.LossFunction;
import com.github.libsml.function.ProgressFunction;
import com.github.libsml.lbfgs.function.VecFreeFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.libsml.Logger.*;

public class LBFGS {

    private static final Logger log = LoggerFactory.getLogger(LBFGS.class);

    public static void lbfgs(int n, float[] x, float[] ptrFx,
                             LossFunction evaluate,
                             ProgressFunction progress,
                             EvaluatorFunction test,
                             VecFreeFunction vecFree,
                             LBFGSParameter param) {



        int i, j, k, ls, bound;
        float[] step = new float[1];

		/* Constant parameters and their default values. */
        param = param == null ? new LBFGSParameter() : param;

        final int m = param.m;

        float[][] b = new float[2 * m + 1][2 * m + 1];
        float[] alpha = new float[2 * m + 1];
        float[] theta = new float[2 * m + 1];

        float[] xp = null;
        float[] g = null, gp = null, pg = null;
        float[] d = null, pf = null;

        float xnorm, gnorm, beta;
        float[] fx = new float[1];
        float rate = 0f;
        LineSearchFunction lineSearch = null;

		/* Check the input parameters for errors. */
        if (n <= 0) {
            throw new IllegalStateException("LBFGSERR_INVALID_N");
        }
        if (param.epsilon < 0) {
            throw new IllegalStateException("LBFGSERR_INVALID_EPSILON");
        }
        if (param.past < 0) {
            throw new IllegalStateException("LBFGSERR_INVALID_TESTPERIOD");
        }
        if (param.delta < 0) {
            throw new IllegalStateException("LBFGSERR_INVALID_DELTA");
        }
        if (param.minStep < 0) {
            throw new IllegalStateException("LBFGSERR_INVALID_MINSTEP");
        }
        if (param.maxStep < 0) {
            throw new IllegalStateException("LBFGSERR_INVALID_MAXSTEP");
        }
        if (param.ftol < 0) {
            throw new IllegalStateException("LBFGSERR_INVALID_FTOL");
        }
        if (param.linesearch == LineSearchConstant.LBFGS_LINESEARCH_BACKTRACKING_WOLFE
                || param.linesearch == LineSearchConstant.LBFGS_LINESEARCH_BACKTRACKING_STRONG_WOLFE) {
            if (param.wolfe <= param.ftol || 1 <= param.wolfe) {
                throw new IllegalStateException("LBFGSERR_INVALID_WOLFE");
            }
        }

        if (param.gtol < 0) {
            throw new IllegalStateException("LBFGSERR_INVALID_GTOL");
        }

        if (param.xtol < 0) {
            throw new IllegalStateException("LBFGSERR_INVALID_XTOL");
        }
        if (param.maxLinesearch <= 0) {
            throw new IllegalStateException("LBFGSERR_INVALID_MAXLINESEARCH");
        }
        if (param.orthantwiseC < 0.) {
            throw new IllegalStateException("LBFGSERR_INVALID_ORTHANTWISE");
        }
        if (param.orthantwiseStart < 0 || n < param.orthantwiseStart) {
            throw new IllegalStateException(
                    "LBFGSERR_INVALID_ORTHANTWISE_START");
        }
        if (param.orthantwiseEnd < 0) {
            param.orthantwiseEnd = n;
        }
        if (n < param.orthantwiseEnd) {
            throw new IllegalStateException("LBFGSERR_INVALID_ORTHANTWISE_END");
        }

        if (param.orthantwiseC != 0.) {
            switch (param.linesearch) {
                case LBFGS_LINESEARCH_BACKTRACKING:
                    lineSearch = new LineSearchOwlqn();
                    break;
                default:
                /* Only the backtracking method is available. */
                    throw new IllegalStateException("LBFGSERR_INVALID_LINESEARCH");
            }
        } else {
            switch (param.linesearch) {
                // case LBFGS_LINESEARCH_MORETHUENTE:
                // linesearch = line_search_morethuente;
                // break;
                case LBFGS_LINESEARCH_BACKTRACKING:
                case LBFGS_LINESEARCH_BACKTRACKING_ARMIJO:
                case LBFGS_LINESEARCH_BACKTRACKING_WOLFE:
                case LBFGS_LINESEARCH_BACKTRACKING_STRONG_WOLFE:

                    lineSearch = new LineSearchBacktracking();
                    break;
                default:
                    throw new IllegalStateException("LBFGSERR_INVALID_LINESEARCH");
            }
        }




        xp = new float[n];
        g = new float[n];
        gp = new float[n];
        d = new float[n];

        if (param.orthantwiseC != 0.) {
            /* Allocate working space for OW-LQN. */
            pg = new float[n];
        }

		/*
         * Allocate an array for storing previous values of the objective
		 * function.
		 */
        if (0 < param.past) {
            pf = new float[param.past];
        }

		/* Evaluate the function value and its gradient. */
        fx[0] = evaluate.lossAndGrad(x, g, 0);

        if (0. != param.orthantwiseC) {
            /*
             * Compute the L1 norm of the variable and add it to the object
			 * value.
			 */
            xnorm = VecUtils.owlqnL1Norm(x, param.orthantwiseStart, param.orthantwiseEnd);
            fx[0] += xnorm * param.orthantwiseC;
            VecUtils.owlqnPseudoGradient(pg, x, g, n, param.orthantwiseC,
                    param.orthantwiseStart, param.orthantwiseEnd);
        }

		/*
         * Compute the L2 norm of the variable and add it to the object value.
		 */
        xnorm = VecUtils.vec2norm(x);
        // fx[0] += 0. == param.orthantwiseC ? xnorm : 0;

		/* Store the initial value of the objective function. */
        if (pf != null) {
            pf[0] = fx[0];
        }

		/*
         * Compute the direction; we assume the initial hessian matrix H_0 as
		 * the identity matrix.
		 */
        if (param.orthantwiseC == 0.) {
            VecUtils.vecncpy(d, g);
        } else {
            VecUtils.vecncpy(d, pg);
        }

        if (param.orthantwiseC == 0.) {
            gnorm = VecUtils.vec2norm(g);
        } else {
            gnorm = VecUtils.vec2norm(pg);
        }

        if (xnorm < 1.0) {
            xnorm = 1f;
        }

        if (gnorm / xnorm <= param.epsilon) {
            log("Initial Convergence");
            if (ptrFx != null) {
                ptrFx[0] = fx[0];
            }
            return;
        }

        step[0] = VecUtils.vec2norminv(d);

        k = 1;
        while (true) {
			/* Store the current position and gradient vectors. */
            VecUtils.veccpy(xp, x);
            VecUtils.veccpy(gp, g);

            // TODO:处理异常?
			/* Search for an optimal step. */
            try {
                if (param.orthantwiseC == 0.) {
                    ls = lineSearch.lineSearch(n, x, fx, g, d, step, xp, gp,
                            k, evaluate, param);
                } else {
                    ls = lineSearch.lineSearch(n, x, fx, g, d, step, xp, pg,
                            k, evaluate, param);
                    VecUtils.owlqnPseudoGradient(pg, x, g, n, param.orthantwiseC,
                            param.orthantwiseStart, param.orthantwiseEnd);
                }
            } catch (IllegalStateException e) {
                log(e.getMessage());
                VecUtils.veccpy(x, xp);
                VecUtils.veccpy(g, gp);

                if (ptrFx != null) {
                    ptrFx[0] = fx[0];
                }
                return;
            }

			/* Compute x and g norms. */
            xnorm = VecUtils.vec2norm(x);
            gnorm = param.orthantwiseC == 0 ? VecUtils.vec2norm(g) : VecUtils.vec2norm(gp);

            if (progress != null) {
                EvaluatorFunction.Statistics statistics=test==null?null:test.evaluate(x, k);
//                EvaluatorFunction.Statistics statistics=null;
                if (progress.progress(x, g, fx[0], xnorm, gnorm, step[0], n, k,
                        ls,statistics) != 0) {
                    log("Progress stop!");
                    if (ptrFx != null) {
                        ptrFx[0] = fx[0];
                    }
                    return;
                }
            }

			/*
			 * Convergence test. The criterion is given by the following
			 * formula: |g(x)| / \max(1, |x|) < \epsilon
			 */
            if (xnorm < 1.0)
                xnorm = 1.0f;
            if (gnorm / xnorm <= param.epsilon) {
				/* Convergence. */
                log("Convergence");

                if (ptrFx != null) {
                    ptrFx[0] = fx[0];
                }
                return;
            }

			/*
			 * Test for stopping criterion. The criterion is given by the
			 * following formula: (f(past_x) - f(x)) / f(x) < \delta
			 */
            if (pf != null) {
				/* We don't test the stopping criterion while k < past. */
                if (param.past <= k) {
					/* Compute the relative improvement from the past. */
                    rate = (pf[k % param.past] - fx[0]) / fx[0];

					/* The stopping criterion. */
                    if (rate < param.delta) {
                        log("Stopping criterion.");

                        if (ptrFx != null) {
                            ptrFx[0] = fx[0];
                        }
                        return;
                    }
                }

				/* Store the current value of the objective function. */
                pf[k % param.past] = fx[0];
            }

            if (param.maxIterations != 0 && param.maxIterations < k + 1) {
				/* Maximum number of iterations. */
                log("Maximum number of iterations.");

                if (ptrFx != null) {
                    ptrFx[0] = fx[0];
                }
                return;
            }

            vecFree.vecsDot(b, k, x, xp, g, gp, param.orthantwiseC == 0 ? g
                    : pg);

            bound = (m <= k) ? m : k;
            // 初始化：p=-g
            for (i = 0; i < 2 * bound + 1; i++) {
                theta[i] = i == 2 * bound ? -1 : 0;
            }

			/*
			 * for i=k-1 to k-m do: alpha[i]=s[i]*p/s[i]*y[i];
			 * p=p-alpha[i]*y[i];
			 */
            for (i = bound - 1; i >= 0; i--) {
                alpha[i] = 0;
                // for (j = 0; j < 2 * m + 1; j++) {
                for (j = 0; j < 2 * bound + 1; j++) {
                    alpha[i] += theta[j] * b[j][i];
                }
                alpha[i] /= b[i][i + bound];
                theta[i + bound] -= alpha[i];
            }

            // p=(s[k-1]*y[k-1]/y[k-1]*y[k-1])*p
            for (i = 0; i < 2 * bound + 1; i++) {
                theta[i] *= b[bound - 1][2 * bound - 1]
                        / b[2 * bound - 1][2 * bound - 1];
            }

			/*
			 * for i=k-1 to k-m do: beta[i]=y[i]*p/s[i]*y[i];
			 * p=p+(alpha[i]-beta)*s[i];
			 */
            for (i = 0; i < bound; i++) {
                beta = 0f;
                // for (j = 0; j < 2 * m + 1; j++) {
                for (j = 0; j < 2 * bound + 1; j++) {
                    beta += theta[j] * b[bound + i][j];
                }
                beta /= b[i][i + bound];
                theta[i] += alpha[i] - beta;
            }


            vecFree.computeDirect(d, theta, k);

			/*
			 * Constrain the search direction for orthant-wise updates.
			 */
            if (param.orthantwiseC != 0.) {
                for (i = param.orthantwiseStart; i < param.orthantwiseEnd; i++) {
                    if (d[i] * pg[i] >= 0) {
                        d[i] = 0;
                    }
                }
            }


            step[0] = 1.f;
            k++;

        }

    }



    // public static float L2Norm(float[] x) {
    // float norm = 0f;
    //
    // for (int i = 0; i < x.length; i++) {
    // norm += 2 * x[i];
    // }
    // return norm;
    // }


}
