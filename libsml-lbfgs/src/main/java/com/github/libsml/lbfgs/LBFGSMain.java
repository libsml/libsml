package com.github.libsml.lbfgs;

import com.github.libsml.Commands;
import com.github.libsml.Config;
import com.github.libsml.MLContext;
import com.github.libsml.Mode;
import com.github.libsml.commons.util.CommandUtils;
import com.github.libsml.lbfgs.core.LBFGS;
import com.github.libsml.lbfgs.core.LBFGSParameter;
import com.github.libsml.lbfgs.core.LineSearchConstant;
import com.github.libsml.lbfgs.function.VecFreeFunction;
import com.github.libsml.lbfgs.function.imp.SingleVecFree;
import com.github.libsml.lbfgs.function.imp.mr.MRVecFree;

/**
 * Created by yellowhuang on 2015/5/19.
 */
public class LBFGSMain {
//    private static final Logger log = LoggerFactory.getLogger(LBFGSMain.class);

    public static LBFGSParameter generateLBFGSParameter(MLContext ctx) {
        Config config = ctx.getConf();
        LBFGSParameter para = new LBFGSParameter();

        para.mode = ctx.getMode();
        para.m = config.getInt("optimization.lbfgs.m", para.m);

        para.epsilon = config.getFloat("optimization.lbfgs.epsilon", para.epsilon);
        para.past = config.getInt("optimization.past", para.past);
        para.delta = config.getFloat("optimization.delta", para.delta);
        para.maxIterations = config.getInt("optimization.max_iterations", para.maxIterations);

        para.maxLinesearch = config.getInt("optimization.linesearch.max_step", para.maxLinesearch);
        para.minStep = config.getFloat("optimization.min_step", para.minStep);
        para.maxStep = config.getFloat("optimization.max_step", para.maxStep);

        para.linesearch = LineSearchConstant.parse(
                config.get("optimization.linesearch", LineSearchConstant.BACKTRACKING));

        para.ftol = config.getFloat("optimization.ftol", para.ftol);
        para.wolfe = config.getFloat("optimization.wolfe", para.wolfe);
        para.gtol = config.getFloat("optimization.gtol", para.gtol);
        para.xtol = config.getFloat("optimization.xtol", para.xtol);
        para.orthantwiseC = config.getFloat("optimization.l1.c", para.orthantwiseC);
        para.orthantwiseStart = config.getInt("optimization.l1.start", para.orthantwiseStart);
        para.orthantwiseEnd = config.getInt("optimization.l1.end", para.orthantwiseEnd);

        return para;
    }

    public static void main(String[] args) {

        CommandUtils.checkArgument(args != null && args.length > 0, Commands.helpString());

        MLContext ctx = new MLContext(args[0]).init();
        LBFGSParameter parameter = generateLBFGSParameter(ctx);


        //n>0,argument check in Config.java
        int featureNum = ctx.getFeatureNum();
        float bias = ctx.getBias();
        if (bias > 0) {
            featureNum++;
        }
        float[] w = new float[featureNum];
        float[] ptrFx = new float[1];

        Mode mode = parameter.mode;
        VecFreeFunction vecFree = null;

        switch (mode) {
            case LOCAL:
                vecFree = new SingleVecFree(parameter.m, featureNum);
                break;
            case MR:
                vecFree = new MRVecFree(ctx.getConf());
                break;
            case SPARK:
                //TODO:
                break;
            default:
                throw new IllegalStateException("Configuraton exception:mode=" + mode);
        }


        LBFGS.lbfgs(featureNum
                , ctx.getPrior(w)
                , ptrFx
                , ctx.getLossFunction()
                , ctx.getProgressFunction()
                , ctx.getEvaluatorFunction()
                , vecFree
                , parameter);
        ctx.destroy();
    }
}
