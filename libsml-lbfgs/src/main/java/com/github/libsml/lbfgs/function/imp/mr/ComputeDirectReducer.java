package com.github.libsml.lbfgs.function.imp.mr;

import com.github.libsml.data.avro.Entry;
import org.apache.avro.mapred.AvroKey;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;


public class ComputeDirectReducer extends Reducer<IntWritable, FloatWritable, AvroKey<Entry>, NullWritable> {

    @Override
    public void reduce(IntWritable key, Iterable<FloatWritable> values, Context context)
            throws IOException, InterruptedException {
        double sum = 0.f;
        for (FloatWritable v : values) {
            sum += v.get();
        }
        if (sum != 0) {
            context.write(new AvroKey<Entry>(new Entry(key.get(), (float)sum)), NullWritable.get());
        }
    }

}
