package com.github.libsml.function.imp.mr;

import com.github.libsml.Job.AbstractJob;
import com.github.libsml.MLContext;
import com.github.libsml.commons.util.HadoopUtils;
import com.github.libsml.data.Datas;
import com.github.libsml.data.avro.CRData;
import com.github.libsml.data.avro.Entry;
import com.github.libsml.function.EvaluatorFunction;
import com.github.libsml.model.Statistics;
import org.apache.avro.mapred.AvroKey;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.github.libsml.StaticParameter.*;

/**
 * Created by yellowhuang on 2015/4/17.
 */
public class MRLogisticRegressionEvaluator extends AbstractJob implements EvaluatorFunction {

//    private final Config config;
    public static final String EVALUATE="evaluate";

    public MRLogisticRegressionEvaluator(MLContext ctx) {
        super(ctx);
    }

    @Override
    public Statistics evaluate(float[] w, int k) {
        try {
            return evaluatWithException(w, k);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e.getMessage());
        } catch (InterruptedException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public Statistics evaluatWithException(float[] w, int k)
            throws IOException, URISyntaxException, ClassNotFoundException, InterruptedException {


        Configuration configuration = getConfiguration(EVALUATE);

        addConfig(configuration,MLContext.DATA_FEATURE_NUMBER, MLContext.DATA_BIAS,MLContext.TEST_THRESHOLD);

        Job job = HadoopUtils.prepareAvroJob(getTestPathsString()
                , getOutPath(TEST_PATH)
                , CRData.getClassSchema()
                , LREvaluatorMapper.class
                , FloatWritable.class
                , FloatWritable.class
                , null
                , LREvaluatorReducer.class
                , Text.class
                , NullWritable.class
                , configuration
                , true);
        job.setNumReduceTasks(1);

        addFloatArrayCacheFile(configuration,w, W_SUB_PATH + "/hv_weight", WEIGHT_LINK, job);

        job.setJobName("iteration_" + k + "_" + job.getJobName());

        waitForCompletion(job);
        return new Statistics(HadoopUtils.readString(getOutPath(TEST_PATH), configuration));


    }

    static class LREvaluatorMapper extends Mapper<AvroKey<CRData>, NullWritable, FloatWritable, FloatWritable> {
        private float[] weight = null;
        private float bias;

        private FloatWritable keyOut = new FloatWritable();
        private FloatWritable valueOut = new FloatWritable();


        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            int featureNum = context.getConfiguration().getInt("data.feature.number", -2);
            bias = context.getConfiguration().getFloat("data.bias", -1);
            if(bias>0){
                featureNum++;
            }
            weight = new float[featureNum];
            // read weightFile in distributed cache
            Datas.readArrayLocal(new Path(WEIGHT_LINK), context.getConfiguration(), weight);
        }

        @Override
        protected void map(AvroKey<CRData> key, NullWritable value, Context context)
                throws IOException, InterruptedException {
            CRData x = key.datum();
            double xv = 0;
            for (Entry entry : x.getFeatures()) {
                xv += entry.getValue() * weight[entry.getIndex()];
            }
            float p = (float) (1 / (Math.exp(-xv) + 1));
            keyOut.set(p);
            valueOut.set(x.getY());
            context.write(keyOut, valueOut);
        }
    }


    static class LREvaluatorReducer extends Reducer<FloatWritable, FloatWritable, Text, NullWritable> {

        private long tp = 0, tn = 0, fp = 0, fn = 0;
        private long aucCount = 0;
        private float threshold;


        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            threshold = context.getConfiguration().getFloat("test.threshold", 0.5f);

        }

        @Override
        protected void reduce(FloatWritable key, Iterable<FloatWritable> values, Context context)
                throws IOException, InterruptedException {
            float p = key.get();
            for (FloatWritable value : values) {
                float v = value.get();
                if (v > 0) {
                    aucCount += (fp + tn);
                }
                if (p >= threshold) {
                    if (v > 0) {
                        tp++;
                    } else {
                        fp++;
                    }
                } else {
                    if (v <= 0) {
                        tn++;
                    } else {
                        fn++;
                    }
                }

            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
//            System.out.println(String.format("aucCount=%d,tp=%d,fp=%d,tn=%d,fn=%d", aucCount, tp, fp, tn, fn));
            float precision = ((float) tp) / (tp + fp);
            float recall = ((float) tp) / (tp + fn);
            float f1Score = 2 * precision * recall / (precision + recall);
            float accucary = ((float) tp + tn) / (tp + fn + tn + fp);
            float auc = ((float) aucCount) / ((fp + tn) * (tp + fn));
            Statistics statistics = new Statistics(precision, recall, f1Score, auc, accucary);
            context.write(new Text(statistics.toString()), NullWritable.get());
        }
    }
}
