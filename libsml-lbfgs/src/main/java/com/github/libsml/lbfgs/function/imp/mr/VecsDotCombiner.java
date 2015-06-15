package com.github.libsml.lbfgs.function.imp.mr;

import com.github.libsml.data.avro.Index;
import org.apache.avro.mapred.AvroKey;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.mahout.math.map.OpenIntDoubleHashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VecsDotCombiner
        extends
        Reducer<AvroKey<Index>, FloatWritable, AvroKey<Index>, FloatWritable> {

//    double sum = 0.f;
    FloatWritable fw = new FloatWritable();
    int index = -1;
    OpenIntDoubleHashMap indexSums = new OpenIntDoubleHashMap();


    @Override
    protected void reduce(AvroKey<Index> key,
                          Iterable<FloatWritable> values, final Context context)
            throws IOException, InterruptedException {
        int i = key.datum().getI();
        index = i;


        int count = 0;
        List<Float> fs = new ArrayList<Float>();

        if(key.datum().getJ()==-1){
            for (FloatWritable v : values) {

                float tmp=v.get();
                indexSums.adjustOrPutValue(index,tmp,tmp);
//                sum += v.get();
            }
            return;
        }

        for (FloatWritable v : values) {
            fs.add(v.get());

            count++;
            if (count > 2) {
                throw new IllegalStateException(
                        String.format("VecsDot combiner exception size:%d of key(i:%d,j:%d) is more than 2."
                                , fs.size(), i, key.datum().getJ()));
            }
        }
        if (fs.size() == 1) {
            fw.set(fs.get(0));
            context.write(key, fw);
        } else {
            double tmp = fs.get(0) * fs.get(1);
            indexSums.adjustOrPutValue(index, tmp, tmp);

        }

    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        for(int k:indexSums.keys().elements()){
            fw.set((float)indexSums.get(k));
            context.write(new AvroKey<Index>(new Index(k, -1)), fw);
        }

    }
}
