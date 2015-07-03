package com.github.libsml.feature.engineering.smooth;

import com.github.libsml.commons.math.Smooth;
import com.github.libsml.commons.util.HadoopUtils;
import com.github.libsml.commons.util.PrintUtils;
import com.google.common.base.Preconditions;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import java.io.IOException;

/**
 * Created by yellowhuang on 2015/6/16.
 */
public class MRBeyesianSmooth {


    public final static String KEY_INDEX = "keyIndex";
    public final static String CLICK_INDEX = "clickIndex";
    public final static String IMPRESSION_INDEX = "impressionIndex";
    public final static String SEP = "sep";
    public final static String SEP_DEFAULT = "\\s+";
    public final static int MAX_LEN = 8000000;


    public static void main(String[] args) throws InterruptedException, IOException, ClassNotFoundException {
        smooth(args);
    }

    public static void smooth(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

        Configuration conf = new Configuration();
        String[] remainArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        Preconditions.checkArgument(remainArgs.length == 2, "MR beyesian smooth exception.");
        Job job = HadoopUtils.prepareJob(new Path(remainArgs[0]), new Path(remainArgs[1]),
                TextInputFormat.class, SmoothMapper.class,
                Text.class, Text.class, SmoothReducer.class, Text.class, Text.class,
                TextOutputFormat.class, conf);

        job.waitForCompletion(true);

    }


    public static class SmoothMapper extends Mapper<LongWritable, Text, Text, Text> {

        private int keyIndex = 0;
        private int clickIndex = 1;
        private int impressionIndex = 2;
        private String sep;
        private Text outKey = new Text();
        private Text outValue = new Text();


        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            keyIndex = context.getConfiguration().getInt(KEY_INDEX, 0);
            clickIndex = context.getConfiguration().getInt(CLICK_INDEX, 1);
            impressionIndex = context.getConfiguration().getInt(IMPRESSION_INDEX, 1);
            sep = context.getConfiguration().get(SEP, SEP_DEFAULT);
        }

        @Override
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            String s = value.toString();
            if ("".equals(s.trim())) {
                return;
            }
            String[] ss = s.split("\\s+");
            Preconditions.checkState(ss.length > keyIndex && ss.length > clickIndex && ss.length > impressionIndex,
                    "MR beyesian smooth exception:out of bound:" + s + ",seq=" + sep);

            outKey.set(ss[keyIndex]);
            outValue.set(ss[clickIndex] + "," + ss[impressionIndex]);
            context.write(outKey, outValue);
        }
    }

    public static class SmoothReducer extends Reducer<Text, Text, Text, Text> {

        double[] smooth = new double[2];
        private Text outValue = new Text();
        double[] clicks = new double[MAX_LEN];
        double[] impressions = new double[MAX_LEN];

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {
            int index = 0;
            for (Text v : values) {
                String[] ss = v.toString().split(",");
                clicks[index] = Double.parseDouble(ss[0]);
                impressions[index] = Double.parseDouble(ss[1]);
                index++;
                if (index >= MAX_LEN) {
                    break;
                }
            }
//            System.out.println("topic:" + key.toString());
//            System.out.println("clicks");
//            PrintUtils.printArray(clicks, 0, index);
//            System.out.println("impressions");
//            PrintUtils.printArray(impressions, 0, index);
            Smooth.bayesianSmooth(clicks, impressions, smooth, index);
//            System.out.println(String.format("alpha=%f,beta=%f", smooth[0], smooth[1]));
            outValue.set(smooth[0] + "\t" + smooth[1]);
            context.write(key, outValue);
        }
    }


}
