package com.github.libsml.function.imp.mr;

import com.github.libsml.StaticParameter;
import com.github.libsml.data.avro.CRData;
import com.github.libsml.data.Datas;
import org.apache.avro.mapred.AvroKey;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;


public class HvMapper extends Mapper<AvroKey<CRData>, NullWritable, IntWritable, DoubleWritable> {



    private float[] weight = null;
    private float[] s = null;
    private double[] hv = null;
    private float bias;
    private double d;



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
        hv = new double[featureNum];
        // read weightFile in distributed cache
        Datas.readArrayLocal(new Path(StaticParameter.WEIGHT_LINK), context.getConfiguration(), weight);
        Datas.readArrayLocal(new Path(MRLogisticRegression.D_LINK), context.getConfiguration(), s);
    }

    @Override
    public void map(AvroKey<CRData> key, NullWritable nullvalue, Context context) {
        CRData data = key.datum();
        float y = data.getY();

        d = (1 / (1 + Math.exp(-MRLogisticRegression.xv(data, weight, bias) * y)));
        d=d*(1-d);

        MRLogisticRegression.xTv(data, MRLogisticRegression.xv(data, s, bias) * data.getWeight() * d, hv, bias);

    }

    @Override
    public void cleanup(Context context) throws IOException, InterruptedException {
        for (int i = 0; i < hv.length; i++) {
            if (hv[i] !=  0.0) {
                context.write(new IntWritable(i), new DoubleWritable(hv[i]));
            }
        }
    }

}
