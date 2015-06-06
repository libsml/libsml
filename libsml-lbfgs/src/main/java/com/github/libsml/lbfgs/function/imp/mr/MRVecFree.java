package com.github.libsml.lbfgs.function.imp.mr;

import com.github.libsml.Config;
import com.github.libsml.commons.util.HadoopUtils;
import com.github.libsml.commons.util.VecUtils;
import com.github.libsml.data.Datas;
import com.github.libsml.data.avro.Entry;
import com.github.libsml.data.avro.Index;
import com.github.libsml.data.avro.VecFreeData;
import com.github.libsml.lbfgs.function.VecFreeFunction;
import org.apache.avro.mapreduce.AvroJob;
import org.apache.avro.mapreduce.AvroKeyInputFormat;
import org.apache.avro.mapreduce.AvroKeyOutputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.compress.DeflateCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.github.libsml.StaticParameter.*;

public class MRVecFree implements VecFreeFunction {


    private final Config config;
    private String rootPath;

    public MRVecFree(Config config) {
        this.config = config;
        rootPath = config.get("output.path");
        rootPath = rootPath.endsWith("/") ? rootPath : rootPath + "/";

    }

    public static int parseIndex(Path path, int bound, int k) {
        String fileName = path.getName().trim();
        String parentName = path.getParent().getName().trim();

        if (PG_SUB_PATH.equals(fileName)) {
            return 2 * bound;
        } else if (S_SUB_PATH.equals(parentName)) {
            return Integer.parseInt(fileName) - k + bound - 1;
        } else if (Y_SUB_PATH.equals(parentName)) {
            return 2 * bound + Integer.parseInt(fileName) - k - 1;
        } else {
            throw new IllegalStateException("Parse Index exception:path="
                    + path.toString());
        }
    }

    @Override
    public void vecsDot(float[][] b, int k, float[] x, float[] xp, float[] g,
                        float[] gp, float[] pg) {
        try {
            vecsDotWithException(b, k, x, xp, g, gp, pg);
        } catch (ClassNotFoundException e) {
            //TODO:
        } catch (InterruptedException e) {
            //TODO:
        } catch (IOException e) {
            //TODO:
        }
    }


    private void vecsDotWithException(float[][] b, int k, float[] x,
                                      float[] xp, float[] g, float[] gp, float[] pg)
            throws IOException, ClassNotFoundException, InterruptedException {


        Job job = Job.getInstance();
        Configuration conf = job.getConfiguration();

        VecUtils.vecdiff(x, x, xp);
        Datas.writeArrayOverwrite(new Path(rootPath, S_SUB_PATH + "/" + k), conf, x);
        VecUtils.vecadd(x, xp, 1);

        VecUtils.vecdiff(g, g, gp);
        Datas.writeArrayOverwrite(new Path(rootPath, Y_SUB_PATH + "/" + k), conf, g);
        VecUtils.vecadd(g, gp, 1);

        Datas.writeArrayOverwrite(new Path(rootPath, PG_SUB_PATH), conf, pg);


        int m = (b.length - 1) / 2;
        int bound = m <= k ? m : k;
        conf.setInt("bound", bound);
        conf.setInt("k", k);

        config.addConfig(conf, HADOOP_PREFIX);
        config.addConfig(conf, HADOOP_PREFIX2);


        job.setJarByClass(MRVecFree.class);
        for (int i = k; i > k - m && i >= 1; i--) {
            FileInputFormat.addInputPath(job, new Path(rootPath, S_SUB_PATH + "/" + i));
            FileInputFormat.addInputPath(job, new Path(rootPath, Y_SUB_PATH + "/" + i));
        }
        FileInputFormat.addInputPath(job, new Path(rootPath, PG_SUB_PATH));

        Path outputPath = new Path(rootPath, VECFREE_DOT_SUB_PATH);
//        Datas.deleteFileIfExist(outputPath, job.getConfiguration());
        HadoopUtils.delete(job.getConfiguration(), outputPath);

        FileOutputFormat.setOutputPath(job, outputPath);
        FileOutputFormat.setCompressOutput(job, true);
        FileOutputFormat.setOutputCompressorClass(job, DeflateCodec.class);

        job.setInputFormatClass(AvroKeyInputFormat.class);
        job.setMapperClass(VecsDotMapper.class);
        AvroJob.setInputKeySchema(job, Entry.SCHEMA$);
        AvroJob.setMapOutputKeySchema(job, Index.SCHEMA$);
        job.setMapOutputValueClass(FloatWritable.class);

        job.setOutputFormatClass(AvroKeyOutputFormat.class);
        job.setReducerClass(VecsDotReducer.class);
        AvroJob.setOutputKeySchema(job, VecFreeData.SCHEMA$);
        job.setOutputValueClass(NullWritable.class);

        job.setCombinerClass(VecsDotCombiner.class);
        job.setPartitionerClass(VecsDotPartitioner.class);

        job.setNumReduceTasks(6 * bound);
        job.setJobName("iteration_" + k + "_" + job.getJobName());

        job.waitForCompletion(true);

        if (k <= m) {
            for (int i = 2 * bound - 3; i >= bound - 1; i--) {
                for (int j = 0; j <= i; j++) {
                    int jt = j >= bound - 1 ? j + 1 : j;
                    b[i + 1][jt] = b[i][j];
                    b[jt][i + 1] = b[j][i];
                }
            }
        } else {
            for (int i = 1; i < bound; i++) {
                for (int j = i; j < 2 * bound; j++) {
                    b[i - 1][j - 1] = b[i][j];
                    b[j - 1][i - 1] = b[j][i];
                }
            }

            for (int i = 1 + bound; i < 2 * bound; i++) {
                for (int j = i; j < 2 * bound; j++) {
                    b[i - 1][j - 1] = b[i][j];
                    b[j - 1][i - 1] = b[j][i];
                }
            }

        }
        Datas.readSymmetryMatrix(outputPath, conf, b);


    }

    @Override
    public void computeDirect(float[] d, float[] theta, int k) {

        try {
            computeDirectWithException(d, theta, k);
        } catch (IOException e) {
            // TODO:
        } catch (URISyntaxException e) {
            // TODO:
        } catch (InterruptedException e) {
            // TODO:
        } catch (ClassNotFoundException e) {
            // TODO:
        }
    }

    private void computeDirectWithException(float[] d, float[] theta, int k)
            throws IOException, URISyntaxException, ClassNotFoundException, InterruptedException {

        Job job = Job.getInstance();
        Configuration conf = job.getConfiguration();

//        Path thetaPath = new Path(rootPath, THETA_SUB_PATH);
//        AvroIOUtils.writeArrayOverwrite(thetaPath, conf, theta);
//        job.addCacheFile(new URI(thetaPath.toString() + "#" + THETA_LINK));
        for (int i = 0; i < theta.length; i++) {
            conf.setFloat(THETA_PREFIX + i, theta[i]);
        }

        config.addConfig(conf, HADOOP_PREFIX);
        config.addConfig(conf, HADOOP_PREFIX3);

        int m = (theta.length - 1) / 2;
        int bound = m <= k ? m : k;
        conf.setInt("bound", bound);
        conf.setInt("k", k);
        conf.setInt("m", m);
        job.setJarByClass(MRVecFree.class);

        for (int i = k; i > k - bound && i >= 1; i--) {
            FileInputFormat.addInputPath(job, new Path(rootPath, S_SUB_PATH + "/" + i));
            FileInputFormat.addInputPath(job, new Path(rootPath, Y_SUB_PATH + "/" + i));
        }
        FileInputFormat.addInputPath(job, new Path(rootPath, PG_SUB_PATH));

        Path outputPath = new Path(rootPath, VECFREE_DIRECT_SUB_PATH);
//        AvroIOUtils.deleteFileIfExist(outputPath, job.getConfiguration());
        HadoopUtils.delete(job.getConfiguration(), outputPath);

        FileOutputFormat.setOutputPath(job, outputPath);
        FileOutputFormat.setCompressOutput(job, true);
        FileOutputFormat.setOutputCompressorClass(job, DeflateCodec.class);

        job.setInputFormatClass(AvroKeyInputFormat.class);
        job.setMapperClass(ComputeDirectMapper.class);
        AvroJob.setInputKeySchema(job, Entry.SCHEMA$);
        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(FloatWritable.class);

        job.setOutputFormatClass(AvroKeyOutputFormat.class);
        job.setReducerClass(ComputeDirectReducer.class);
        AvroJob.setOutputKeySchema(job, Entry.SCHEMA$);
        job.setOutputValueClass(NullWritable.class);

        job.setCombinerClass(ComputeDirectCombiner.class);
        job.setJobName("iteration_" + k + "_" + job.getJobName());

        job.waitForCompletion(true);

        for (int i = 0; i < d.length; i++) {
            d[i] = 0;
        }

        Datas.readArray(new Path(rootPath, VECFREE_DIRECT_SUB_PATH), conf, d);
    }
}
