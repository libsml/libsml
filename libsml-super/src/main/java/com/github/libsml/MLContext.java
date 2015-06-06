package com.github.libsml;


import com.github.libsml.data.Datas;
import com.github.libsml.data.liblinear.DataPoint;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.rdd.RDD;

import java.io.IOException;

/**
 * Created by huangyu on 15/6/6.
 */
public class MLContext {

    public static final String DATA_FEATURE_NUMBER = "data.feature.number";
    public static final String DATA_BIAS = "data.bias";
    public static final String W_PATH = "w.path";
    public static final String OPTIMIZATION_L2_C = "optimization.l2.c";

    private final Config conf;
    private SparkContext sparkContext;
    private RDD<DataPoint> inputDataRDD;


    public MLContext(Config conf) {
        this.conf = conf;
    }

    public MLContext(String confPath) {

        this.conf = Config.createFromFile(confPath);
    }


    public RDD<DataPoint> inputDataRDD(){
        if(inputDataRDD==null){
            //TODO:
        }
        return inputDataRDD;
    }

    public SparkContext sparkContext() {
        if (sparkContext == null) {
            sparkContext = new SparkContext();
        }
        return sparkContext;
    }

    public SparkConf sparkConf() {
        if (sparkContext == null) {
            sparkContext = new SparkContext();
        }
        return sparkContext.getConf();
    }


    public int getFeatureNum() {
        return getConf().getInt(DATA_FEATURE_NUMBER, -1);
    }

    public float getBias() {
        return getConf().getFloat(DATA_BIAS, -1);
    }

    public float getL2C() {
        return getConf().getFloat(OPTIMIZATION_L2_C, 1f);
    }

    public float[] getPrior(float[] w) {
        //w.path
        String mode = getMode();
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

    public boolean isSaveIterationResult(){
        return false;
    }

    public String getMode() {
        return conf.get("mode", "local");
    }

    public Config getConf() {
        return conf;
    }
}
