package com.github.libsml.lbfgs.function.imp.mr;

import com.github.libsml.data.avro.Index;
import org.apache.avro.mapred.AvroKey;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.mapreduce.Partitioner;

public class VecsDotPartitioner extends
        Partitioner<AvroKey<Index>, FloatWritable> {

    @Override
    public int getPartition(AvroKey<Index> key, FloatWritable value,
                            int numPartitions) {
        /*
        int m = (numPartitions / 3 - 1) / 2;
		int i = key.datum().getI();
		int j = key.datum().getJ();
		if (i == m - 1) {
			i = 0;
		} else if (i == 2 * m - 1) {
			i = 1;
		} else if (i == 2 * m) {
			i = 2;
		} else {
			throw new IllegalStateException(String.format(
					"Partition exception i=%d,m=%d,numPartitons=%d", i, m,
					numPartitions));
		}
		return i * (2 * m + 1) + j;
		*/
        int i = key.datum().getI();
        if (i > numPartitions) {
            throw new IllegalStateException(String.format(
                    "Partition exception i=%d,numPartitons=%d", i,
                    numPartitions));
        }
        return i;
    }

}
