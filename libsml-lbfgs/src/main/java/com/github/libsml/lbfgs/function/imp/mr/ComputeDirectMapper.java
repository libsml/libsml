package com.github.libsml.lbfgs.function.imp.mr;

import com.github.libsml.StaticParameter;
import com.github.libsml.data.avro.Entry;
import org.apache.avro.mapred.AvroKey;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import java.io.IOException;

public class ComputeDirectMapper extends
		Mapper<AvroKey<Entry>, NullWritable, IntWritable, FloatWritable> {

	private float[] theta;
	private int indexI = -1;
	private int bound = -1;
	private int k = -1;
    private int m =-1;

	private IntWritable keyOut = new IntWritable();
	private FloatWritable valueOut = new FloatWritable();

	private void checkFields() {
		if (indexI < 0 || bound < 0 || k < 0||m<0) {
			throw new IllegalStateException(String.format(
					"VecFree Mapper fields exception:bound=%d,k=%d,indexI=%d,m=%d",
					bound, k, indexI,m));
		}
	}

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        Path path = ((FileSplit) context.getInputSplit()).getPath();
        k = context.getConfiguration().getInt("k", -1);
        bound = context.getConfiguration().getInt("bound", -1);
        m = context.getConfiguration().getInt("m", -1);

        indexI = MRVecFree.parseIndex(path, bound, k);
        System.out.println("indexI:"+indexI);

        checkFields();
        theta = new float[2 * m + 1];

        for(int i=0;i<2*bound+1;i++){
            theta[i]=context.getConfiguration().getFloat(StaticParameter.THETA_PREFIX +i,0);
        }

//        AvroIOUtils.readArrayLocal(new Path(THETA_LINK),
//                context.getConfiguration(), theta);
    }

	@Override
	protected void map(AvroKey<Entry> key, NullWritable value, Context context)
			throws IOException, InterruptedException {

		Entry entry = key.datum();
		keyOut.set(entry.getIndex());
		valueOut.set(entry.getValue() * theta[indexI]);
		context.write(keyOut, valueOut);
	}

}
