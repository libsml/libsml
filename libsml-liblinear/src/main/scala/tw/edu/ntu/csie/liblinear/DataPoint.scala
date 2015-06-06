package tw.edu.ntu.csie.liblinear

import org.apache.spark.SparkException
import org.apache.spark.mllib.linalg.{DenseVector, SparseVector, Vector}
import org.apache.spark.mllib.regression.LabeledPoint


/**
 * DataPoint represents a sparse data point with label.
 * @param index
 * @param value
 * @param y
 */
class DataPoint(val index : Array[Int], val value : Array[Double], val y : Double) extends Serializable
{
	def getMaxIndex() : Int =
	{
		if(this.index.isEmpty)
		{
			return 0
		}
		this.index.last
	}

	def genTrainingPoint(n : Int, b : Double, posLabel : Double) : DataPoint =
	{
		var index : Array[Int] = null
		var value : Array[Double] = null
		var y = if(this.y == posLabel) 1.0 else -1.0
		if(b < 0)
		{
			index = this.index
			value = this.value
		}
		else
		{
			val length = this.index.length
			index = new Array[Int](length + 1)
			value = new Array[Double](length + 1)
			this.index.copyToArray(index, 0)
			this.value.copyToArray(value, 0)
			index(length) = n-1
			value(length) = b
		}
		new DataPoint(index, value, y)
	}
}

object DataPoint
{
	implicit def fromLabeledPoint(input : LabeledPoint) : DataPoint =
	{
		input.features match {
			case dx: DenseVector => {
				val n = dx.size
				new DataPoint(0 until n toArray, dx.values, input.label)
			}
			case sx: SparseVector => {
				new DataPoint(sx.indices, sx.values, input.label)
			}
			case _ => throw new SparkException(s"fromMLlib doesn't support ${input.features.getClass}.")
		}
	}

	implicit def fromVector(input : Vector) : DataPoint =
	{
		input match {
			case dx: DenseVector => {
				val n = dx.size
				new DataPoint(0 until n toArray, dx.values, 0)
			}
			case sx: SparseVector => {
				new DataPoint(sx.indices, sx.values, 0)
			}
			case _ => throw new SparkException(s"fromMLlib doesn't support ${input.getClass}.")
		}
	}

	def fromMLlib(input : LabeledPoint) : DataPoint =
	{
		fromLabeledPoint(input)
	}

}
