package com.github.libsml.function.imp.mr;

import com.github.libsml.StaticParameter;
import com.github.libsml.data.Datas;
import com.github.libsml.data.avro.CRData;
import org.apache.avro.mapred.AvroKey;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

public class MRLogisticRegressionMapper extends Mapper<AvroKey<CRData>, NullWritable, IntWritable, DoubleWritable> {

    private float[] weight = null;
    private double loss = 0.0f;
    private double[] gradient = null;
    private float bias;

    @Override
    public void setup(Context context) throws IOException {

        //maxFeature>0 argument check in Config.java
        int featureNum = context.getConfiguration().getInt("data.feature.number", -2);
        bias = context.getConfiguration().getFloat("data.bias", -1);
        if (bias > 0) {
            featureNum++;
        }
        weight = new float[featureNum];
        gradient = new double[featureNum];
        // read weightFile in distributed cache
        Datas.readArrayLocal(new Path(StaticParameter.WEIGHT_LINK), context.getConfiguration(), weight);
    }

    // loss = add( w[i] * log(1+exp(-y[i]* weightT * x[i])) ) + 0.5 * lbfgs_l2_c * ||weight||2
    @Override
    public void map(AvroKey<CRData> key, NullWritable nullvalue, Context context) {
        CRData data = key.datum();

        float w = data.getWeight();
        float y = data.getY();


        double yz= MRLogisticRegression.xv(data, weight, bias)*y;

        if (yz >= 0)
            loss += Math.log(1 + Math.exp(-yz))*w;
        else
            loss += (-yz + Math.log(1 + Math.exp(yz)))*w;

        double z = 1 / (1 + Math.exp(-yz));

        MRLogisticRegression.xTv(data, y * (z - 1) * w, gradient, bias);

//        double decisionValue = 0.0f;
//        for (Entry e : l) {
//            decisionValue += e.getValue() * weight[e.getIndex()];
//        }
//
//        if (bias > 0) {
//            decisionValue += bias * weight[weight.length - 1];
//        }
//
//        double lossTemp;
//        lossTemp = y * decisionValue;
//        if (lossTemp > 0) {
//            lossTemp = Math.log(1 + Math.exp(-lossTemp));
//        } else {
//            lossTemp = (-lossTemp) + Math.log(1 + Math.exp(lossTemp));
//        }
//        loss += w * lossTemp;
//
//        double gradientTemp =1 / (1 + Math.exp(-y * decisionValue)) - 1 * y * w;
//        for (Entry e : l) {
//            gradient[e.getIndex()] += gradientTemp * e.getValue();
//        }
    }

    @Override
    public void cleanup(Context context) throws IOException, InterruptedException {
        context.write(new IntWritable(-1), new DoubleWritable( loss));
        for (int i = 0; i < gradient.length; i++) {
            if (gradient[i] !=  0.0) {
                context.write(new IntWritable(i), new DoubleWritable(gradient[i]));
            }
        }
    }

}
