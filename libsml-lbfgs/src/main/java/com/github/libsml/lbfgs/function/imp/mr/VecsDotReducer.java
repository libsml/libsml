package com.github.libsml.lbfgs.function.imp.mr;

import com.github.libsml.data.avro.Index;
import com.github.libsml.data.avro.VecFreeData;
import org.apache.avro.mapred.AvroKey;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by huangyu on 15/4/6.
 */
public class VecsDotReducer
        extends
        Reducer<AvroKey<Index>, FloatWritable, AvroKey<VecFreeData>, NullWritable> {

    private double sum = 0.f;
    private int index = -1;
    private int bound = -1;

    private void checkFields() {
        if (bound < 0) {
            throw new IllegalStateException(String.format(
                    "VecFree Reducer fields exception:bound=%d",
                    bound));
        }
    }

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        bound = context.getConfiguration().getInt("bound", -1);
        checkFields();
    }

    @Override
    protected void reduce(AvroKey<Index> key,
                          Iterable<FloatWritable> values, Context context)
            throws IOException, InterruptedException {

        int i = key.datum().getI();
        index = i;
        int j = key.datum().getJ();

        if (j == -1) {
            for (FloatWritable v : values) {
                sum += v.get();
            }
        } else {
            int count = 0;
            List<Float> fs = new ArrayList<Float>();
            for (FloatWritable v : values) {
                fs.add(v.get());

                count++;
                if (count > 2) {
                    throw new IllegalStateException(
                            String.format(
                                    "VecsDot reducer exception size:%d of key(i:%d,j:%d) is more than 2."
                                    , fs.size(), i, j)
                    );
                }
            }
            if (fs.size() == 2) {
                sum += fs.get(0) * fs.get(1);
            }
        }

    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        if (index == -1) {
            throw new IllegalStateException("VecDot reduce has nothing!");
        }
        int i = 0;
        int j = 0;
        if (index >= 0 && index <= 2 * bound) {
            i = bound - 1;
            j = index;
        } else if (index >= 2 * bound + 1 && index <= 3 * bound - 1) {
            i = 2 * bound - 1;
            j = index - 2 * bound - 1;
        } else if (index >= 3 * bound && index <= 4 * bound) {
            i = 2 * bound - 1;
            j = index - 2 * bound;
        } else if (index >= 4 * bound + 1 && index <= 5 * bound - 1) {
            i = 2 * bound;
            j = index - 4 * bound - 1;
        } else if (index >= 5 * bound && index <= 6 * bound - 2) {
            i = 2 * bound;
            j = index - 4 * bound;
        } else if (index == 6 * bound - 1) {
            i = 2 * bound;
            j = 2 * bound;
        } else {
            throw new IllegalStateException("VecsDot reducer exception in clean up: bound:" + bound + " outofbound.");
        }
        context.write(new AvroKey<VecFreeData>(new VecFreeData(new Index(i, j), (float)sum)), NullWritable.get());
    }
}
