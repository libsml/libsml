package tw.edu.ntu.csie.liblinear

import java.io.{FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}

import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.GeneralizedLinearModel
import tw.edu.ntu.csie.liblinear.SolverType._

/**
 * A linear model stores weights and other information.
 *
 *@param param user-specified parameters

 */
class LiblinearModel(val param : Parameter, labelSet : Array[Double]) extends Serializable
{
	var label : Array[Double] = labelSet.sortWith(_ < _)
	val nrClass : Int = label.size
	var subModels : Array[GeneralizedLinearModel] = null
	var bias : Double = -1.0
	var threshold : Double = 0.0

	def setBias(b : Double) : this.type =
	{
		this.bias = b
		this
	}

	def predictValues(index : Array[Int], value : Array[Double]) : Array[Double] =
	{
		subModels.map(model => model.predict(Vectors.sparse(model.weights.size, index,value)))
	}

	/**
	 *Predict a label given a DataPoint.
	 *@param point a DataPoint
	 *@return a label
	 */
	def predict(point : DataPoint) : Double =
	{
		val decValues = predictValues(point.index, point.value)
		var labelIndex = 0
		if(nrClass == 2)
		{
			if(decValues(0) < threshold)
			{
				labelIndex = 1
			}
		}
		else
		{
			var i = 1
			while(i < nrClass)
			{
				if(decValues(i) > decValues(labelIndex))
				{
					labelIndex = i
				}
				i += 1
			}
		}
		label(labelIndex)
	}

	/**
	 *Predict probabilities given a DataPoint.
	 *@param point a DataPoint
	 *@return probabilities which follow the order of label
	 */
	def predictProbability(point : DataPoint) : Array[Double] =
	{
		Predef.require(param.solverType == L2_LR, "predictProbability only supports for logistic regression.")
		var probEstimates = predictValues(point.index, point.value)
		// Already prob value in MLlib
		if(nrClass == 2)
		{
			probEstimates = probEstimates :+ 1.0 - probEstimates(0)
		}
		else
		{
			var sum = probEstimates.sum
			probEstimates = probEstimates.map(value => value/sum)
		}
		probEstimates
	}

	/**
	 * Save Model to the local file system.
	 *
	 * @param fileName path to the output file
	 */
	def saveModel(fileName : String) =
	{
		val fos = new FileOutputStream(fileName)
		val oos = new ObjectOutputStream(fos)
		oos.writeObject(this)
		oos.close
	}
}

object LiblinearModel
{

	/**
	 * load Model from the local file system.
	 *
	 * @param fileName path to the input file
	 */
	def loadModel(fileName : String) : LiblinearModel =
	{
		val fis = new FileInputStream(fileName)
		val ois = new ObjectInputStream(fis)
		val model : LiblinearModel = ois.readObject.asInstanceOf[LiblinearModel]
		ois.close
		model
	}
}
