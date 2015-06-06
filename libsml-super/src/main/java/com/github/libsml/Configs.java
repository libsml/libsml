package com.github.libsml;

import com.github.libsml.function.ProgressFunction;
import com.github.libsml.function.imp.SingleLogisticRegressionEvaluator;
import com.github.libsml.function.imp.mr.MRLogisticRegressionEvaluator;
import com.github.libsml.commons.util.HadoopUtils;
import com.github.libsml.commons.util.LocalFileUtils;
import com.github.libsml.data.Datas;
import com.github.libsml.data.liblinear.Problem;
import com.github.libsml.function.EvaluatorFunction;
import com.github.libsml.function.LossFunction;
import com.github.libsml.function.imp.Progress;
import com.github.libsml.function.imp.SingleLogisticRegressionLoss;
import com.github.libsml.function.imp.mr.MRLogisticRegression;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * Created by yellowhuang on 2015/5/20.
 */
public class Configs {

    public static final String DATA_FEATURE_NUMBER="data.feature.number";
    public static final String DATA_BIAS="data.bias";
    public static final String W_PATH="w.path";

    public static int getFeatureNum(Config conf) {
        return conf.getInt(DATA_FEATURE_NUMBER, -1);
    }

    public static int getBias(Config conf) {
        return conf.getInt(DATA_BIAS, -1);
    }

    public static float[] getPrior(float[] w, Config conf) {
        //w.path
        String mode = getMode(conf);
        String wPathString = conf.get(W_PATH);
        if ("mr".equals(mode) && wPathString != null) {
            Path wPath = wPathString != null ? new Path(wPathString) : null;
            try {
                Datas.readArray(wPath, new Configuration(), w);
            } catch (IOException e) {
                throw new IllegalStateException("Configuration exception:w.path=" + wPathString);
            }
        }
        return w;
    }

    public static String getMode(Config conf) {
        return conf.get("mode", "local");
    }

    public static String getInputPaths(Config conf) {
        return conf.get("input.paths");
    }



    public static String getOutputPath(Config conf) {
        return conf.get("output.path");
    }

    public static void outputPath(Config conf) {
        String mode = getMode(conf);
        try {
            if ("local".equals(mode)) {
                LocalFileUtils.mkdir(conf.get("output.path"), conf.getBoolean("output.force.overwrite", true));
            } else if ("mr".equals(mode)) {
                HadoopUtils.mkdir(new Path(conf.get("output.path"))
                        , conf.getBoolean("output.force.overwrite", true)
                );
            }
        } catch (IOException e) {
            throw new IllegalStateException("Mkdir exception:" + e.getMessage());
        }
    }

    private static Problem getProblem(Config conf) throws IOException {
        String dataFormat = conf.get("data.format", "avro");
        float bias = getBias(conf);
        if ("avro".equals(dataFormat)) {
            return Problem.loadAvroData(bias, conf.get("input.paths"));
        } else if ("libSVM".equals(dataFormat)) {
            return Problem.loadLibSVMData(bias, conf.get("input.paths"));
        } else {
            throw new IllegalStateException(
                    "Configuration parse exception:data.format="
                            + dataFormat);
        }
    }

    public static EvaluatorFunction getEvaluatorFunction(Config conf) {
        EvaluatorFunction test = null;

        String mode = conf.get("mode", "local");
        float bias = getBias(conf);

        try {
            if ("local".equals(mode)) {

                test = conf.contains("test.paths") ? new SingleLogisticRegressionEvaluator(
                        getProblem(conf), conf.getFloat("test.threshold", 0.5f)) : null;

            } else if ("mr".equals(mode)) {
//            test = conf.contains("test.paths") ? new SingleLogisticRegressionEvaluator(conf) : null;
                test = conf.contains("test.paths") ? new MRLogisticRegressionEvaluator(conf) : null;

            } else {
                throw new IllegalStateException("Configuration exception: loss.mode=" + mode);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Configuration exception:generate loss function " + e.getMessage());
        }
        return test;

    }

    public static LossFunction getLossFunction(Config conf) {
        String lossMode = conf.get("mode", "local");
        float bias = getBias(conf);
        LossFunction loss = null;
        try {
            if ("local".equals(lossMode)) {
                loss = new SingleLogisticRegressionLoss(getProblem(conf)
                        , conf.getFloat("optimization.l2.c", 0f));
            } else if ("mr".equals(lossMode)) {
                loss = new MRLogisticRegression(conf);
            } else {
                throw new IllegalStateException("Configuration exception: loss.mode=" + lossMode);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Configuration exception:generate loss function " + e.getMessage());
        }
        return loss;
    }

    public static ProgressFunction getProgressFunction(Config conf) {
        return new Progress(conf);
    }

}
