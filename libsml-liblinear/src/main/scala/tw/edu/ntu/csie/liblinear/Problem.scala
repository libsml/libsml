package tw.edu.ntu.csie.liblinear

import org.apache.spark.rdd.RDD

/**
 * A problem stores data points and other necessary information.
 *

 */
class Problem() extends Serializable
{
	var dataPoints : RDD[DataPoint] = null
	var n : Int = 0
	var l : Long = 0
	var bias : Double = -1.0

	def setData(dataPoints : RDD[DataPoint]) : this.type =
	{
		this.dataPoints = dataPoints
		this.l = dataPoints.count()
		this.n = this.dataPoints.map(p => p.getMaxIndex()).reduce(math.max(_, _)) + 1
		if(this.bias >= 0)
		{
			this.n += 1
		}
		this
	}

	def genBinaryProb(posLabel : Double) : Problem =
	{
		var binaryProb = new Problem()
		binaryProb.l = this.l
		binaryProb.n = this.n
		binaryProb.bias = this.bias
		/* Compute Problem Label */
		val dataPoints = this.dataPoints.mapPartitions(blocks => {
			blocks.map(p => p.genTrainingPoint(this.n, this.bias, posLabel))
		}).cache()
		binaryProb.dataPoints = dataPoints
		binaryProb
	}
}
