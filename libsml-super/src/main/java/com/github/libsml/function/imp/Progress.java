package com.github.libsml.function.imp;


import com.github.libsml.MLContext;
import com.github.libsml.Mode;
import com.github.libsml.function.ProgressFunction;
import com.github.libsml.commons.util.PrintUtils;
import com.github.libsml.function.EvaluatorFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by yellowhuang on 2015/4/13.
 */
public class Progress implements ProgressFunction {

    private static final Logger log = LoggerFactory.getLogger(Progress.class);

    public static final int FEATURE_PRINT_NUM = 10;
    //    private PrintStream log;
    private final MLContext ctx;
    private String outString;

    private float bestAuc = Float.MIN_VALUE;
    private int bestIndex = 1;


    public Progress(MLContext ctx) {
        this.ctx = ctx;

        outString = ctx.getOutputPath();
        outString = outString.endsWith("/") ? outString : outString + "/";
//        File outFile = new File(outString);
//        if (outFile.exists() && !config.getBoolean("output.force.overwrite", true)) {
//            outString = null;
//        } else if (!outFile.exists()) {
//            outFile.mkdir();
//        }
    }


    @Override
    public int progress(float[] x, float[] g, float fx, float xnorm, float gnorm, float step,
                        int n, int k, int ls, EvaluatorFunction.Statistics statistics) {
        com.github.libsml.Logger.log(stringFormat(x, g, fx, xnorm, gnorm, step, n, k, ls, statistics));
//        Logger.logger.print(stringFormat(x, g, fx, xnorm, gnorm, step, n, k, ls, statistics));
        if (outString != null && Mode.LOCAL==ctx.getMode()) {
            String fileString = outString + k;
            File file = new File(fileString);
            try {

                PrintUtils.printFloatArray(new PrintStream(file), x, ",");

            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage());
            }

        }
        return 0;
    }

    private String stringFormat(float[] x, float[] g, float fx, float xnorm, float gnorm,
                                float step, int n, int k, int ls, EvaluatorFunction.Statistics statistics) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String format = String.format("Iteration %d at time %s:\n\tfx=%f,xnorm=%f,gnorm=%f,ls=%d,step=%f,\n\t",
                k, dateFormat.format(new Date()), fx, xnorm, gnorm, ls, step);

        format += stringFormatVec(x, "x") + "\t" + stringFormatVec(g, "g");
        if (statistics != null) {
            if (bestAuc < statistics.getAuc()) {
                bestAuc = statistics.getAuc();
                bestIndex = k;
            }
            format += String.format("\tEvaluat result:\n\t\tprecision=%f,recall=%f" +
                    ",f1Score=%f,accuracy=%f,auc=%f,bestMode=%d\n"
                    , statistics.getPrecision(), statistics.getRecall(), statistics.getF1Score()
                    , statistics.getAccuracy(), statistics.getAuc(), bestIndex);
        }

        return format;

    }

    private String stringFormatVec(float[] vec, String desc) {
        String format = "";
        format += String.format("%s size=%d\n\t\t", desc, vec.length);

        int step = vec.length / FEATURE_PRINT_NUM;
        step = step == 0 ? 1 : step;

        for (int i = 0; i < vec.length; i += step) {
            format += String.format("%s[%d]=%f  ", desc, i, vec[i]);
        }
        format += System.lineSeparator();
        return format;
    }
}
