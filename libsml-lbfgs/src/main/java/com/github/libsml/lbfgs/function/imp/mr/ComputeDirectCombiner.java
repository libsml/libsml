package com.github.libsml.lbfgs.function.imp.mr;

import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;


public class ComputeDirectCombiner extends Reducer<IntWritable, FloatWritable, IntWritable, FloatWritable> {

    private FloatWritable f = new FloatWritable();

    @Override
    public void reduce(IntWritable key, Iterable<FloatWritable> values, Context context)
            throws IOException, InterruptedException {
        double sum = 0.f;
        for (FloatWritable v : values) {
            sum += v.get();
        }
        if (sum != 0) {
            f.set((float) sum);
            context.write(key, f);
        }
    }

}
