package tw.edu.ntu.csie.liblinear

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

/**
 *	Some useful methods
 */

object Utils
{

	/**
	 * Load data in the LIBSVM format into an RDD[DataPoint].
	 *
	 * @param sc SparkContext
	 * @param path path to the input data files
	 * @return an RDD of DataPoint
	 */
	def loadLibSVMData(sc : SparkContext, path : String) : RDD[DataPoint] =
	{
		loadLibSVMData(sc, path, sc.defaultMinSplits)
	}
	/**
	 * Load data in the LIBSVM format into an RDD[DataPoint].
	 *
	 * @param sc SparkContext
	 * @param path path to the input data files
	 * @param numPartitions number of partitions
	 * @return an RDD of DataPoint
	 */
	def loadLibSVMData(sc : SparkContext, path : String, numPartitions : Int) : RDD[DataPoint] =
	{
		val parsed = sc.textFile(path, numPartitions).map(_.trim).filter(!_.isEmpty)
		parsed.map(line => {
			val tokens = line.split(" |\t|\n")
			val n = tokens.size - 1
			val y = tokens.head.toDouble
			var index = new Array[Int](n)
			var value = new Array[Double](n)
			for(i <- 1 to n)
			{
				var pair = tokens(i).split(":")
				index(i-1) = pair(0).toInt-1
				value(i-1) = pair(1).toDouble
			}
			new DataPoint(index, value, y)
		})
	}
}
