package com.github.libsml.function.imp.mr;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;


public class MRLogisticRegressionLessMemoryCombiner extends Reducer<IntWritable, DoubleWritable, IntWritable, DoubleWritable> {


    @Override
    public void reduce(IntWritable key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {
        double sum = 0;
        for (DoubleWritable v : values) {
            sum += v.get();
        }
        context.write(key, new DoubleWritable(sum));
    }

}
