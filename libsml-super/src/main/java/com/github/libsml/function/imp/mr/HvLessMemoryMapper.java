package com.github.libsml.function.imp.mr;


import com.github.libsml.StaticParameter;
import com.github.libsml.data.avro.CRData;
import com.github.libsml.data.avro.Entry;
import com.github.libsml.data.Datas;
import org.apache.avro.mapred.AvroKey;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

/**
 * Created by yellowhuang on 2015/4/16.
 */
public class HvLessMemoryMapper extends Mapper<AvroKey<CRData>, NullWritable, IntWritable, DoubleWritable> {

    private float[] weight = null;
    private float[] s = null;
    private float bias;
    private double d;

    private IntWritable keyOut = new IntWritable();
    private DoubleWritable valueOut = new DoubleWritable();

    @Override
    public void setup(Context context) throws IOException {

        //maxFeature>0 argument check in Config.java
        int featureNum = context.getConfiguration().getInt("data.feature.number", -2);
        bias = context.getConfiguration().getFloat("data.bias", -1);
        if (bias > 0) {
            featureNum++;
        }
        weight = new float[featureNum];
        s = new float[featureNum];
        // read weightFile in distributed cache
        Datas.readArrayLocal(new Path(StaticParameter.WEIGHT_LINK), context.getConfiguration(), weight);
        Datas.readArrayLocal(new Path(MRLogisticRegression.D_LINK), context.getConfiguration(), s);
    }

    // loss = add( w[i] * log(1+exp(-y[i]* weightT * x[i])) ) + 0.5 * lbfgs_l2_c * ||weight||2
    @Override
    public void map(AvroKey<CRData> key, NullWritable nullvalue, Context context) throws IOException, InterruptedException {
        CRData data = key.datum();

        float y = data.getY();

        d =  1 / (1 + Math.exp(-MRLogisticRegression.xv(data, weight, bias) * y));
        d=d*(1-d);
        d= MRLogisticRegression.xv(data, s, bias) * data.getWeight() * d;

        for (Entry f : data.getFeatures()) {

            keyOut.set(f.getIndex());
            valueOut.set(d*f.getValue());
            context.write(keyOut,valueOut);
        }
        keyOut.set(s.length - 1);
        valueOut.set(bias*d);
        context.write(keyOut, valueOut);
    }

}