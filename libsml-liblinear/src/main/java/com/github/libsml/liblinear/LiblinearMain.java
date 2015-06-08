package com.github.libsml.liblinear;

import com.github.libsml.Commands;
import com.github.libsml.Config;
import com.github.libsml.MLContext;
import com.github.libsml.commons.util.CommandUtils;
import com.github.libsml.liblinear.core.LiblinearParameter;
import com.github.libsml.liblinear.core.Tron;

/**
 * Created by huangyu on 15/5/23.
 */
public class LiblinearMain {


    private static LiblinearParameter generateParameter(MLContext ctx) {

        Config config=ctx.getConf();
        LiblinearParameter para = new LiblinearParameter();

        para.epsilon = config.getFloat("optimization.liblinear.epsilon", para.epsilon);
        para.maxIterations = config.getInt("optimization.max_iterations", para.maxIterations);
//        para.orthantwiseC = config.getFloat("optimization.l1.c", para.orthantwiseC);
//        para.orthantwiseStart = config.getInt("optimization.l1.start", para.orthantwiseStart);
//        para.orthantwiseEnd = config.getInt("optimization.l1.end", para.orthantwiseEnd);

        return para;
    }


    public static void main(String[] args) {

        CommandUtils.checkArgument(args != null && args.length > 0, Commands.helpString());

        MLContext ctx = new MLContext(args[0]).init();

        Tron tron = new Tron(ctx.getLossFunction()
                , ctx.getProgressFunction()
                , ctx.getEvaluatorFunction()
                , generateParameter(ctx));

        int featureNum = ctx.getFeatureNum();
        float bias = ctx.getBias();
        if (bias > 0) {
            featureNum++;
        }
        float[] w = new float[featureNum];
        tron.tron(ctx.getPrior(w));

        ctx.destroy();
    }
}
