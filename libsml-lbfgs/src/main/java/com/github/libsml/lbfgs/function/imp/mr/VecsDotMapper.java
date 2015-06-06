package com.github.libsml.lbfgs.function.imp.mr;

import com.github.libsml.data.avro.Entry;
import com.github.libsml.data.avro.Index;
import org.apache.avro.mapred.AvroKey;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import java.io.IOException;

public class VecsDotMapper
        extends
        Mapper<AvroKey<Entry>, NullWritable, AvroKey<Index>, FloatWritable> {
    private int indexI = -1;
    private int bound = -1;
    private int k = -1;
    private FloatWritable fw = new FloatWritable();

    private void checkFields() {
        if (indexI < 0 || bound < 0 || k < 0 || indexI > 2 * bound) {
            throw new IllegalStateException(String.format(
                    "VecFree Mapper fields exception:bound=%d,k=%d,indexI=%d",
                    bound, k, indexI));
        }
    }

    @Override
    protected void setup(Context context) throws IOException,
            InterruptedException {
        Path path = ((FileSplit) context.getInputSplit()).getPath();
        k = context.getConfiguration().getInt("k", -1);
        bound = context.getConfiguration().getInt("bound", -1);

        indexI = MRVecFree.parseIndex(path, bound, k);
        System.out.println("indexI:"+indexI);

        checkFields();

    }

    @Override
    protected void map(AvroKey<Entry> key, NullWritable value, Context context)
            throws IOException, InterruptedException {

        Entry entry = key.datum();
        int j = entry.getIndex();
        float v = entry.getValue();
        if (v==0.f){
            return;
        }
        fw.set(v);

        if (indexI == bound - 1) {
            for (int i = 0; i <= 2 * bound; i++) {
                context.write(new AvroKey<Index>(new Index(i, j)), fw);
            }
            context.write(new AvroKey<Index>(new Index(bound - 1, j)), fw);
        } else if (indexI == 2 * bound - 1) {
            context.write(new AvroKey<Index>(new Index(2 * bound - 1, j)), fw);
            for (int i = 2 * bound + 1; i <= 4 * bound; i++) {
                context.write(new AvroKey<Index>(new Index(i, j)), fw);
            }
            context.write(new AvroKey<Index>(new Index(4 * bound - 1, j)), fw);
        } else if (indexI == 2 * bound) {

            context.write(new AvroKey<Index>(new Index(2 * bound, j)), fw);
            context.write(new AvroKey<Index>(new Index(4 * bound, j)), fw);
            for (int i = 4 * bound + 1; i <= 6 * bound - 1; i++) {
                context.write(new AvroKey<Index>(new Index(i, j)), fw);
            }
            context.write(new AvroKey<Index>(new Index(6 * bound-1, j)), fw);
        } else {
            int offset = indexI <= bound - 2 ? 1 : 0;
            context.write(new AvroKey<Index>(new Index(indexI, j)), fw);
            context.write(new AvroKey<Index>(new Index(indexI + 2 * bound + offset, j)), fw);
            context.write(new AvroKey<Index>(new Index(indexI + 4 * bound + offset, j)), fw);
        }

    }
}
