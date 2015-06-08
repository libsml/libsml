package com.github.libsml;


import com.github.libsml.commons.util.HadoopUtils;
import com.github.libsml.commons.util.LocalFileUtils;
import com.github.libsml.data.liblinear.DataPoint;
import com.github.libsml.data.liblinear.Problem;
import com.github.libsml.function.EvaluatorFunction;
import com.github.libsml.function.LossFunction;
import com.github.libsml.function.ProgressFunction;
import com.github.libsml.function.imp.Progress;
import com.github.libsml.function.imp.SingleLogisticRegressionEvaluator;
import com.github.libsml.function.imp.SingleLogisticRegressionLoss;
import com.github.libsml.function.imp.mr.MRLogisticRegression;
import com.github.libsml.function.imp.mr.MRLogisticRegressionEvaluator;
import com.github.libsml.function.imp.spark.SparkLogisticRegression;
import com.github.libsml.model.Models;
import com.google.common.base.Preconditions;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.rdd.RDD;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.github.libsml.Mode.LOCAL;


/**
 * Created by huangyu on 15/6/6.
 */
public class MLContext {


    public static final String LOCAL_TEXT = "local_text";
    public static final String LOCAL_AVRO = "local_avro";
    public static final String HDFS_TEXT = "hdfs_text";
    public static final String HDFS_AVRO = "hdfs_avro";
    public static final String AVRO = "avro";
    public static final String TEXT = "text";
    public static final String HADOOP = "hadoop";
    public static final String LIBSVM = "libSVM";


    public static final String DATA_FEATURE_NUMBER = "data.feature.number";
    public static final int DATA_FEATURE_NUMBER_DEFAULT = -1;
    public static final String DATA_BIAS = "data.bias";
    public static final int DATA_BIAS_DEFAULT = -1;

    public static final String MODEL_PRIOR_PATH = "model.prior.path";


    /**
     * local_text,local_avro,hdfs_text or hdfs_avro
     */
    public static final String MODEL_PRIOR_MODE = "model.prior.mode";
    public static final String MODEL_PRIOR_MODE_DEFAULT = HDFS_AVRO;


    public static final String OPTIMIZATION_L2_C = "optimization.l2.c";
    public static final float OPTIMIZATION_L2_C_DEFAULT = 1f;
    public static final String MODE = "mode";
    public static final String MODE_DEFAULT = LOCAL.getName();
    public static final String INPUT_PATHS = "input.paths";
    public static final String OUTPUT_PATH = "output.path";
    public static final String TEST_PATH = "test.paths";
    public static final String OUTPUT_ITERATION_SAVE = "output.iteration.save";
    public static final boolean OUTPUT_ITERATION_SAVE_DEFAULT = false;
    public static final String OUTPUT_ITERATION_MODE = "output.iteration.mode";
    public static final String OUTPUT_ITERATION_MODE_DEFAULT = AVRO;
    public static final String LOG_FILE = "log.file";
    public static final String OUTPUT_FORCE_OVERWRITE = "output.force.overwrite";
    public static final String MEMORY_LESS = "memory.less";
    public static final boolean MEMORY_LESS_DEFAULT = false;
    public static final boolean OUTPUT_FORCE_OVERWRITE_DEFAULT = true;

    public static final String DATA_FORMAT = "data.format";
    public static final String DATA_FORMAT_DEFAULT = "avro";

    public static final String TEST_THRESHOLD = "test.threshold";
    public static final float TEST_THRESHOLD_DEFAULT = 0.5f;


    private final Config conf;
    private SparkContext sparkContext;
    private Map<String, Configuration> hadoopConfs;
    private RDD<DataPoint> inputDataRDD;

    public MLContext(String confPath) {
        this.conf = Config.createFromFile(confPath);
    }


    public MLContext init() {
        initOtherContext();
        checkArgument();
        com.github.libsml.Logger.initLogger(LOG_FILE);
        outputPath();
        return this;
    }

    public void destroy() {

        if (inputDataRDD != null) {
            //TODO:
        }
    }


    public RDD<DataPoint> getInputDataRDD() {
        return inputDataRDD;
    }

    private void initOtherContext() {
        Mode mode = getMode();
        switch (mode) {
            case SPARK:
                initSparkContext();
                break;
            case MR:
                initHadoopContext();
                break;
            default:
        }

    }

    private SparkContext initSparkContext() {
        Preconditions.checkState(sparkContext == null, "Spark context exception:spark context has inited!");
        sparkContext = new SparkContext();
        //TODO:
        return sparkContext;
    }

    private void initHadoopContext() {
        Preconditions.checkState(hadoopConfs == null, "Hadoop context exception:hadoop context has inited!");
        hadoopConfs = new HashMap<String, Configuration>();
        Map<String, String> configMap = conf.configMap;
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String[] keys = entry.getKey().split("\\.");
            if (keys.length > 2 && keys[1].equals(HADOOP)) {
                Configuration hadoopConf = hadoopConfs.get(keys[0]);
                if (hadoopConf == null) {
                    hadoopConf = new Configuration();
                    hadoopConfs.put(keys[0], hadoopConf);
                }
                hadoopConf.set(entry.getKey().replace(keys[0] + "." + keys[1] + ".", ""), entry.getValue());
            }

        }
        addHadoopConfigAll(HADOOP);

        for (Map.Entry<String, Configuration> entry : hadoopConfs.entrySet()) {
            entry.getValue().set("mapreduce.job.name", entry.getKey());
        }
    }

    public Configuration getHadoopConfWithInitation(String name) {
        Configuration hadoopConf = hadoopConfs.get(name);
        if (hadoopConf == null) {
            hadoopConf = new Configuration();
            hadoopConfs.put(name, hadoopConf);
        }
        hadoopConf.set("mapreduce.job.name", name);
        addHadoopConfig(hadoopConf, HADOOP);
        return hadoopConf;
    }

    private void addHadoopConfigAll(String prefix) {
        for (Configuration hadoopConf : hadoopConfs.values()) {
            addHadoopConfig(hadoopConf, prefix);
        }
    }

    private void addHadoopConfig(Configuration hadoopConf, String prefix) {
        prefix += ".";
        Map<String, String> configMap = conf.configMap;
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                hadoopConf.set(entry.getKey().replace(prefix, ""), entry.getValue());
            }
        }
    }


    public SparkContext sparkContext() {
        return sparkContext;
    }

    public SparkConf sparkConf() {
        return sparkContext.getConf();
    }


    public int getFeatureNum() {
        return getConf().getInt(DATA_FEATURE_NUMBER, DATA_FEATURE_NUMBER_DEFAULT);
    }

    public float getBias() {
        return getConf().getFloat(DATA_BIAS, DATA_BIAS_DEFAULT);
    }

    public float getL2C() {
        return getConf().getFloat(OPTIMIZATION_L2_C, OPTIMIZATION_L2_C_DEFAULT);
    }

    public float[] getPrior(float[] w) {
        String wPathString = conf.get(MODEL_PRIOR_PATH);

        if (wPathString != null) {
            String modelMode = conf.get(MODEL_PRIOR_MODE, MODEL_PRIOR_MODE_DEFAULT);
            if (LOCAL_TEXT.equals(modelMode)) {
                Models.readModelFromLocalText(wPathString, w);
            } else if (LOCAL_AVRO.equals(modelMode)) {
                Models.readModelFromLocalAvro(wPathString, w);
            } else if (HDFS_TEXT.equals(modelMode)) {
                Models.readModelFromHDFSText(wPathString, w);
            } else if (HDFS_AVRO.equals(modelMode)) {
                Models.readModelFromHDFSAvro(wPathString, w);
            } else {
                throw new IllegalArgumentException(String.format(
                        "Configuration exception:%s=%s", MODEL_PRIOR_MODE, modelMode));
            }
        }
        return w;
    }

    public String getInputPaths() {
        return conf.get(INPUT_PATHS);
    }

    public String getOutputPath() {
        return conf.get(OUTPUT_PATH);
    }

    public String getTestPath() {
        return conf.get(TEST_PATH);
    }

    public boolean isSaveIterationResult() {
        return conf.getBoolean(OUTPUT_ITERATION_SAVE, OUTPUT_ITERATION_SAVE_DEFAULT);
    }

    public String getSaveIterationResultMode() {
        return conf.get(OUTPUT_ITERATION_MODE, OUTPUT_ITERATION_MODE_DEFAULT);
    }

    public Mode getMode() {
        return Mode.getByName(conf.get(MODE, MODE_DEFAULT));
    }

    public boolean isOverwrite() {
        return conf.getBoolean(OUTPUT_FORCE_OVERWRITE, OUTPUT_FORCE_OVERWRITE_DEFAULT);
    }

    public boolean isLessMemory() {
        return conf.getBoolean(MEMORY_LESS, MEMORY_LESS_DEFAULT);
    }

    private void outputPath() {
        Mode mode = getMode();
        try {
            switch (mode) {
                case LOCAL:
                    LocalFileUtils.mkdir(getOutputPath(), isOverwrite());
                    break;
                case MR:
                case SPARK:
                    HadoopUtils.mkdir(new Path(getOutputPath()), isOverwrite());
                    break;
                default:
            }
        } catch (IOException e) {
            throw new IllegalStateException("Mkdir exception:" + e.getMessage());
        }
    }


    private Problem getProblem() throws IOException {
        String dataFormat = conf.get(DATA_FORMAT, DATA_FORMAT_DEFAULT);
        float bias = getBias();
        if (AVRO.equals(dataFormat)) {
            return Problem.loadAvroData(bias, getInputPaths());
        } else if (LIBSVM.equals(dataFormat)) {
            return Problem.loadLibSVMData(bias, getInputPaths());
        } else {
            throw new IllegalStateException(
                    "Configuration parse exception:data.format="
                            + dataFormat);
        }
    }

    public EvaluatorFunction getEvaluatorFunction() {
        EvaluatorFunction test;

        Mode mode = getMode();

        try {
            switch (mode) {
                case LOCAL:
                    test = conf.contains(TEST_PATH) ? new SingleLogisticRegressionEvaluator(
                            getProblem(), conf.getFloat(TEST_THRESHOLD, TEST_THRESHOLD_DEFAULT)) : null;
                    break;
                case MR:
                    test = conf.contains(TEST_PATH) ? new MRLogisticRegressionEvaluator(this) : null;
                    break;
                case SPARK:
                    //TODO:
                    test=null;
                    break;
                default:
                    throw new IllegalStateException("Configuration exception: mode=" + mode);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Configuration exception:generate loss function " + e.getMessage());
        }
        return test;

    }

    public LossFunction getLossFunction() {
        Mode mode = getMode();
        LossFunction loss;
        try {
            switch (mode) {
                case LOCAL:
                    loss = new SingleLogisticRegressionLoss(getProblem()
                            , getL2C());
                    break;
                case MR:
                    loss = new MRLogisticRegression(this);
                    break;
                case SPARK:
                    loss = new SparkLogisticRegression(this);
                    break;
                default:
                    throw new IllegalStateException("Configuration exception: mode=" + mode);
            }

        } catch (IOException e) {
            throw new IllegalStateException("Configuration exception:generate loss function " + e.getMessage());
        }
        return loss;
    }

    public  ProgressFunction getProgressFunction() {
        return new Progress(this);
    }

    public Config getConf() {
        return conf;
    }

    private void checkArgument() {

        Preconditions.checkArgument(conf.contains(INPUT_PATHS)
                , "Configuration check exception:%s is needed.", INPUT_PATHS);
        Preconditions.checkArgument(conf.contains(OUTPUT_PATH)
                , "Configuration check exception:%s is needed.", OUTPUT_PATH);
        Preconditions.checkArgument(conf.contains(DATA_FEATURE_NUMBER)
                , "Configuration check exception:%s is needed.", DATA_FEATURE_NUMBER);

        int fn = conf.getInt(DATA_FEATURE_NUMBER, DATA_FEATURE_NUMBER_DEFAULT);
        Preconditions.checkArgument(fn > 0
                , "Configuration check exception:%s=%d is less than 0.", DATA_FEATURE_NUMBER, fn);

        Mode mode = getMode();
        Preconditions.checkArgument(mode != null
                , "Configuration check exception:%s=%s", MODE, conf.get(MODE, MODE_DEFAULT));

    }

    public static void main(String[] args) {
        MLContext ctx = new MLContext("src/conf/conf");
        ctx.init();
        System.out.println("data.bias:" + ctx.getBias());
        System.out.println("input.paths:" + ctx.getInputPaths());
        System.out.println("mode:" + ctx.getMode());
        System.out.println("overwrite:" + ctx.isOverwrite());
//        System.out.println("lg.hadoop.mapreduce.job.reduces:"
//                + ctx.getHadoopConfWithInitation("lg").get("mapreduce.job.reduces"));
//        System.out.println("hv.mapreduce.mapreduce.input.fileinputformat.split.maxsize:"
//                + ctx.getHadoopConfWithInitation("hv").get("mapreduce.input.fileinputformat.split.maxsize"));
        ctx.destroy();
    }
}
