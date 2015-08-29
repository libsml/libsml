package com.github.libsml.liblinear.core;

import com.github.libsml.Logger;
import com.github.libsml.function.EvaluatorFunction;
import com.github.libsml.function.LossFunction;
import com.github.libsml.function.ProgressFunction;
import com.github.libsml.model.Statistics;

import static com.github.libsml.commons.util.VecUtils.*;

/**
 * Trust Region Newton Method optimization
 */
public class Tron {

    private final LossFunction loss;
    private final ProgressFunction progress;
    private final EvaluatorFunction evaluator;
    private final LiblinearParameter parameter;


    public Tron(final LossFunction loss
            , ProgressFunction progress
            , EvaluatorFunction evaluator
            , LiblinearParameter parameter
    ) {
        this.loss = loss;
        this.progress = progress;
        this.evaluator = evaluator;
        this.parameter = parameter;
    }

    public void tron(float[] w) {
        // Parameters for updating the iterates.
        float eta0 = 1e-4f, eta1 = 0.25f, eta2 = 0.75f;

        // Parameters for updating the trust region size delta.
        float sigma1 = 0.25f, sigma2 = 0.5f, sigma3 = 4;

        int n = w.length;
        int cg_iter;
        float delta, snorm, one = 1.0f;
        float alpha, f, fnew, prered, actred, gs;
        int search = 1, iter;
        float[] s = new float[n];
        float[] r = new float[n];
        float[] w_new = new float[n];
        float[] g = new float[n];

        float[] g_new = new float[n];

//        for (i = 0; i < n; i++) {
//            w[i] = 0;
//        }

        iter = 1;

        f = loss.lossAndGrad(w, g, iter);


        delta = euclideanNorm(g);


        float gnorm1 = delta;
        float gnorm = gnorm1;

        float xnorm;

        if (gnorm <= parameter.epsilon * gnorm1) search = 0;


        while (iter <= parameter.maxIterations && search != 0) {
            cg_iter = trcg(delta, w, g, s, r, iter);

            snorm = euclideanNorm(s);


            System.arraycopy(w, 0, w_new, 0, n);
            vecadd(w_new, s, one);


            gs = vecdot(g, s);
            prered = -0.5f * (gs - vecdot(s, r));
            fnew = loss.lossAndGrad(w_new, g_new, iter);


            // Compute the actual reduction.
            actred = f - fnew;

            // On the first iteration, adjust the initial step bound.
//            snorm = euclideanNorm(s);
            if (iter == 1) delta = Math.min(delta, snorm);

            // Compute prediction alpha*snorm of the step.
            if (fnew - f - gs <= 0)
                alpha = sigma3;
            else
                alpha = (float) Math.max(sigma1, -0.5 * (gs / (fnew - f - gs)));

            // Update the trust region bound according to the ratio of actual to
            // predicted reduction.
            if (actred < eta0 * prered)
                delta = Math.min(Math.max(alpha, sigma1) * snorm, sigma2 * delta);
            else if (actred < eta1 * prered)
                delta = Math.max(sigma1 * delta, Math.min(alpha * snorm, sigma2 * delta));
            else if (actred < eta2 * prered)
                delta = Math.max(sigma1 * delta, Math.min(alpha * snorm, sigma3 * delta));
            else {
                delta = Math.max(delta, Math.min(alpha * snorm, sigma3 * delta));
            }


            xnorm = euclideanNorm(w);
            if (progress != null) {
                Statistics statistics = evaluator == null ? null : evaluator.evaluate(w, iter);
//                EvaluatorFunction.Statistics statistics=null;
                if (progress.progress(w, g, f, xnorm, gnorm, delta, n, iter,
                        cg_iter, statistics) != 0) {
                    Logger.log("Progress stop!");
                    return;
                }
            }
            Logger.log("iter %2d act %5.3e pre %5.3e delta %5.3e f %5.3e |g| %5.3e CG %3d\n"
                    , iter, actred, prered, delta, f, gnorm, cg_iter);

            if (actred > eta0 * prered) {
                iter++;
                System.arraycopy(w_new, 0, w, 0, n);
                System.arraycopy(g_new, 0, g, 0, n);
                f = fnew;

                gnorm = euclideanNorm(g);
                if (gnorm <= parameter.epsilon * gnorm1) {
                    Logger.log("Convergence");
                    break;
                }

//                if (xnorm < 1.0)
//                    xnorm = 1.0f;
//                if (gnorm / xnorm <= parameter.epsilon) {
//                /* Convergence. */
//                    log("Convergence");
//                    return;
//                }
            }

            if (f < -1.0e+32) {
                Logger.log("WARNING: f < -1.0e+32\n");
                break;
            }
            if (Math.abs(actred) <= 0 && prered <= 0) {
                Logger.log("WARNING: actred and prered <= 0\n");
                break;
            }
            if (Math.abs(actred) <= 1.0e-12 * Math.abs(f) && Math.abs(prered) <= 1.0e-12 * Math.abs(f)) {
                Logger.log("WARNING: actred and prered too small\n");
                break;
            }
        }
    }

    private int trcg(float delta, float[] w, float[] g, float[] s, float[] r, int iter) {
        int n = g.length;
        float one = 1;
        float[] d = new float[n];
        float[] Hd = new float[n];
        float rTr, rnewTrnew, cgtol;

        for (int i = 0; i < n; i++) {
            s[i] = 0;
            r[i] = -g[i];
            d[i] = r[i];
        }
        cgtol = 0.1f * euclideanNorm(g);

        int cg_iter = 0;
        rTr = vecdot(r, r);

        while (true) {



            if (euclideanNorm(r) <= cgtol) break;
            cg_iter++;
            loss.Hv(w, d, Hd, iter, cg_iter);

            float alpha = rTr / vecdot(d, Hd);
            vecadd(s, d, alpha);

            if (euclideanNorm(s) > delta) {
                Logger.log("cg reaches trust region boundary\n");
                alpha = -alpha;
                vecadd(s, d, alpha);

                float std = vecdot(s, d);
                float sts = vecdot(s, s);
                float dtd = vecdot(d, d);
                float dsq = delta * delta;
                float rad = (float) Math.sqrt(std * std + dtd * (dsq - sts));
                if (std >= 0)
                    alpha = (dsq - sts) / (std + rad);
                else
                    alpha = (rad - std) / dtd;
                vecadd(s, d, alpha);
                alpha = -alpha;
                vecadd(r, Hd, alpha);
                break;
            }
            alpha = -alpha;
            vecadd(r, Hd, alpha);
            rnewTrnew = vecdot(r, r);
            float beta = rnewTrnew / rTr;
            vecSale(d, beta);
            vecadd(d, r, one);
            rTr = rnewTrnew;
//            log(""+cg_iter+"\n");
//            PrintUtils.printFloatArray(Hd);
        }

        return (cg_iter);
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
