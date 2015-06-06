package com.github.libsml.function.imp.mr;

import com.github.libsml.Config;
import com.github.libsml.Job.AbstractJob;
import com.github.libsml.StaticParameter;
import com.github.libsml.data.avro.CRData;
import com.github.libsml.data.avro.Entry;
import com.github.libsml.commons.util.HadoopUtils;
import com.github.libsml.data.Datas;
import com.github.libsml.function.LossFunction;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Random;

public class MRLogisticRegression extends AbstractJob implements LossFunction {


    public static final String HV_SUB_PATH = "hv";
    public static final String D_SUB_PATH = "d";
    public static final String D_LINK = "s_link";

    // need : data.feature.number,optimization.l2.c,output.path,input.paths,data.model
//    private final Config config;

    public MRLogisticRegression(Config config) {
        super(config);
    }

    public static void main(String[] args) throws IOException,
            ClassNotFoundException, InterruptedException, URISyntaxException {
//		Map<String, String> config = new HashMap<String, String>();
        Config config = Config.createFromFile("conf");


        MRLogisticRegression lr = new MRLogisticRegression(config);

        Random rand = new Random(1);
        float[] w = new float[123 + 1];
        for (int i = 0; i < w.length; i++) {
            w[i] = rand.nextFloat();
        }
        float[] g = new float[123 + 1];

        System.out.println(Arrays.toString(w));
        System.out.println(lr.lossAndGrad(w, g, 1));
        System.out.println(Arrays.toString(g));
    }

    @Override
    public float lossAndGrad(float[] w, float[] g, int k) {
        try {
            return lossAndGradWithException(w, g, k);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void Hv(float[] w, float[] s, float[] Hs, int k, int cgIter) {

        try {
            HvWithException(w, s, Hs, k, cgIter);
        } catch (Exception e) {
            throw new IllegalStateException("Job exceptions:Hv job " + e.getMessage(), e);
        }
    }

    private void HvWithException(float[] w, float[] d, float[] Hs, int k, int cgIter)
            throws IOException, URISyntaxException, ClassNotFoundException, InterruptedException {

        addConfigWithPrefix(StaticParameter.HADOOP_PREFIX, StaticParameter.HADOOP_PREFIX2);
        addConfig("data.feature.number", "data.bias", "optimization.l2.c");


        Job job = HadoopUtils.prepareAvroJob(getDataPathsString()
                , getOutPath(HV_SUB_PATH)
                , CRData.getClassSchema()
                , lessMemory() ? HvLessMemoryMapper.class : HvMapper.class
                , IntWritable.class
                , DoubleWritable.class
                , lessMemory() ? MRLogisticRegressionLessMemoryCombiner.class : null
                , HvReducer.class
                , Entry.getClassSchema()
                , NullWritable.class
                , getConfiguration()
                , true);

        addFloatArrayCacheFile(d, D_SUB_PATH, D_LINK, job);
        if (cgIter == 1) {
            addFloatArrayCacheFile(w, StaticParameter.W_SUB_PATH + "/hv_weight", StaticParameter.WEIGHT_LINK, job);
        } else {
            job.addCacheFile(new URI(getOutPath(StaticParameter.W_SUB_PATH + "/hv_weight").toString() + "#" + StaticParameter.WEIGHT_LINK));
        }
        job.setJobName("Iteration_" + k + "_" + cgIter + "_" + job.getJobName());
        waitForCompletion(job);
        Datas.readArray(getOutPath(HV_SUB_PATH), getConfiguration(), Hs);

    }

    private float lossAndGradWithException(float[] w, float[] g, int k)
            throws IOException, URISyntaxException, ClassNotFoundException,
            InterruptedException {


        addConfigWithPrefix(StaticParameter.HADOOP_PREFIX, StaticParameter.HADOOP_PREFIX1);
        addConfig("data.feature.number", "data.bias", "optimization.l2.c");

        Job job = HadoopUtils.prepareAvroJob(getDataPathsString()
                , getOutPath(StaticParameter.G_SUB_PATH + "/" + k)
                , CRData.getClassSchema()
                , lessMemory() ? MRLogisticRegressionLessMemoryMapper.class : MRLogisticRegressionMapper.class
                , IntWritable.class
                , DoubleWritable.class
                , lessMemory() ? MRLogisticRegressionLessMemoryCombiner.class : null
                , MRLogisticRegressionReducer.class
                , Entry.getClassSchema()
                , NullWritable.class
                , getConfiguration()
                , true);

        addFloatArrayCacheFile(w, StaticParameter.W_SUB_PATH + "/" + k, StaticParameter.WEIGHT_LINK, job);

        job.setJobName("iteration_" + k + "_" + job.getJobName());

        waitForCompletion(job);
        // read g from hdfs
        float loss = Datas.readGradientAndLoss(getOutPath(StaticParameter.G_SUB_PATH + "/" + k), getConfiguration(), g);

        return loss;

    }

    public static double xv(CRData x, float[] v, float b) {
        double xv = 0;
        for (Entry f : x.getFeatures()) {
            xv += v[f.getIndex()] * f.getValue();
        }
        if (b > 0) {
            xv += v[v.length - 1] * b;
        }
        return xv;
    }

    public static void xTv(CRData x, float v, double[] xTv, float b) {
        for (Entry f : x.getFeatures()) {
            xTv[f.getIndex()] += v * f.getValue();
        }
        xTv[xTv.length - 1] += b * v;
    }

    public static void xTv(CRData x, double v, double[] xTv, float b) {
        for (Entry f : x.getFeatures()) {
            xTv[f.getIndex()] += v * f.getValue();
        }
        xTv[xTv.length - 1] += b * v;
    }
}
