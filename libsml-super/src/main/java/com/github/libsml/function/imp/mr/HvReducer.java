package com.github.libsml.function.imp.mr;

import com.github.libsml.data.avro.Entry;
import com.github.libsml.data.Datas;
import org.apache.avro.mapred.AvroKey;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;


public class HvReducer extends Reducer<IntWritable, DoubleWritable, AvroKey<Entry>, NullWritable> {

    private float[] s = null;
    private float l2C = 0.0f;

    @Override
    public void run(Context context) throws IOException, InterruptedException {
        super.run(context);
    }

    @Override
    public void setup(Context context) throws IOException {
        // get lbfgs_data_max_index and lbfgs_l2_c
        l2C = context.getConfiguration().getFloat("optimization.l2.c", 1.0f);
        int featureNum = context.getConfiguration().getInt("data.feature.number", -2);
        s = new float[featureNum+1];
        // read weightFile in distributed cache
        Datas.readArrayLocal(new Path(MRLogisticRegression.D_LINK), context.getConfiguration(), s);
    }

    // loss = add( w[i] * log(1+exp(-y[i]* weightT * x[i])) ) + 0.5 * lbfgs_l2_c * ||weight||2
    @Override
    public void reduce(IntWritable key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {

            double g = 0.0;
            for (DoubleWritable f : values) {
                g += f.get();
            }
            if (l2C > 0) {
                g += l2C * s[key.get()];
            }
            Entry e = new Entry(key.get(), (float) g);
            context.write(new AvroKey<Entry>(e), NullWritable.get());
    }

}
